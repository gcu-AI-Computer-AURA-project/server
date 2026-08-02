package com.AURA.AURA_Service.scan.service;

import com.AURA.AURA_Service.auth.domain.User;
import com.AURA.AURA_Service.auth.repository.UserRepository;
import com.AURA.AURA_Service.common.CustomException;
import com.AURA.AURA_Service.common.ErrorCode;
import com.AURA.AURA_Service.scan.domain.AnalysisCandidate;
import com.AURA.AURA_Service.scan.domain.ScanJob;
import com.AURA.AURA_Service.scan.domain.ScanJob.JobStatus;
import com.AURA.AURA_Service.scan.domain.ScannedItem;
import com.AURA.AURA_Service.scan.domain.ScannedItem.ItemSource;
import com.AURA.AURA_Service.scan.repository.AnalysisCandidateRepository;
import com.AURA.AURA_Service.scan.repository.ScanJobRepository;
import com.AURA.AURA_Service.scan.repository.ScannedItemRepository;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;
import org.springframework.stereotype.Service;
import org.springframework.transaction.support.TransactionTemplate;

@Service
public class ScanJobExecutionService {
	private final TransactionTemplate transactionTemplate;
	private final ScanJobRepository scanJobRepository;
	private final UserRepository userRepository;
	private final ScannedItemRepository scannedItemRepository;
	private final AnalysisCandidateRepository analysisCandidateRepository;
	private final GoogleMetadataCollector googleMetadataCollector;
	private final GeminiSemanticAnalyzer geminiSemanticAnalyzer;
	private final AnalysisDecisionEngine analysisDecisionEngine;

	public ScanJobExecutionService(TransactionTemplate transactionTemplate, ScanJobRepository scanJobRepository,
		UserRepository userRepository, ScannedItemRepository scannedItemRepository,
		AnalysisCandidateRepository analysisCandidateRepository, GoogleMetadataCollector googleMetadataCollector,
		GeminiSemanticAnalyzer geminiSemanticAnalyzer, AnalysisDecisionEngine analysisDecisionEngine) {
		this.transactionTemplate = transactionTemplate;
		this.scanJobRepository = scanJobRepository;
		this.userRepository = userRepository;
		this.scannedItemRepository = scannedItemRepository;
		this.analysisCandidateRepository = analysisCandidateRepository;
		this.googleMetadataCollector = googleMetadataCollector;
		this.geminiSemanticAnalyzer = geminiSemanticAnalyzer;
		this.analysisDecisionEngine = analysisDecisionEngine;
	}

	public void execute(Long scanJobId) {
		ScanExecutionContext context = startScan(scanJobId);
		if (context == null) return;
		try {
			List<CollectedItem> collectedItems = googleMetadataCollector.collect(context.userId(), context.condition());
			List<ScannedItem> scannedItems = saveScannedItems(scanJobId, context.userId(), collectedItems);
			if (geminiSemanticAnalyzer.isRequiredButNotConfigured()) throw new CustomException(ErrorCode.GEMINI_API_CALL_FAILED);
			GeminiAnalysisBundle analysisBundle = geminiSemanticAnalyzer.analyze(scannedItems, context.condition());
			List<CandidateDecision> decisions = analysisDecisionEngine.decide(scannedItems, context.condition(), analysisBundle,
				context.userEmail());
			saveCandidatesAndFinish(scanJobId, decisions, analysisBundle);
		} catch (RuntimeException exception) {
			failScan(scanJobId, exception);
		}
	}

	private ScanExecutionContext startScan(Long scanJobId) {
		return transactionTemplate.execute(status -> {
			ScanJob scanJob = findScanJob(scanJobId);
			if (scanJob.getJobStatus() != JobStatus.PENDING) return null;
			scanJob.markScanning();
			User user = scanJob.getUser();
			return new ScanExecutionContext(user.getUserId(), user.getEmail(),
				ScanCondition.fromSnapshot(scanJob.getConditionSnapshot()));
		});
	}

	private List<ScannedItem> saveScannedItems(Long scanJobId, Long userId, List<CollectedItem> collectedItems) {
		return transactionTemplate.execute(status -> {
			ScanJob scanJob = findScanJob(scanJobId);
			User user = userRepository.findById(userId).orElseThrow(() -> new CustomException(ErrorCode.USER_NOT_FOUND));
			List<ScannedItem> scannedItems = collectedItems.stream()
				.map(item -> item.toScannedItem(scanJob, user))
				.toList();
			List<ScannedItem> savedItems = scannedItemRepository.saveAll(scannedItems);
			int mailCount = countBySource(savedItems, ItemSource.GMAIL);
			int driveCount = countBySource(savedItems, ItemSource.DRIVE);
			scanJob.markAnalyzing(mailCount, driveCount);
			return savedItems;
		});
	}

	private void saveCandidatesAndFinish(Long scanJobId, List<CandidateDecision> decisions, GeminiAnalysisBundle analysisBundle) {
		transactionTemplate.executeWithoutResult(status -> {
			ScanJob scanJob = findScanJob(scanJobId);
			Map<Long, ScannedItem> itemMap = scannedItemRepository.findAllById(decisions.stream()
					.map(CandidateDecision::itemId)
					.toList())
				.stream()
				.collect(Collectors.toMap(ScannedItem::getItemId, Function.identity()));
			List<AnalysisCandidate> candidates = decisions.stream()
				.map(decision -> toAnalysisCandidate(scanJob, itemMap.get(decision.itemId()), decision, analysisBundle))
				.toList();
			analysisCandidateRepository.saveAll(candidates);

			long candidateCount = candidates.stream().filter(candidate -> !candidate.isProtected()).count();
			long protectedCount = candidates.stream().filter(AnalysisCandidate::isProtected).count();
			long estimatedReclaimBytes = candidates.stream()
				.filter(candidate -> !candidate.isProtected())
				.mapToLong(AnalysisCandidate::getEstimatedReclaimBytes)
				.sum();
			if (analysisBundle.fallback()) {
				scanJob.markPartialFailed(toInt(candidateCount), toInt(protectedCount), estimatedReclaimBytes,
					analysisBundle.errorMessage());
				return;
			}
			scanJob.markCompleted(toInt(candidateCount), toInt(protectedCount), estimatedReclaimBytes);
		});
	}

	private AnalysisCandidate toAnalysisCandidate(ScanJob scanJob, ScannedItem item, CandidateDecision decision,
		GeminiAnalysisBundle analysisBundle) {
		return AnalysisCandidate.create(scanJob, item, decision.category(), decision.riskLevel(), decision.priorityScore(),
			decision.ghostScore(), decision.isProtected(), decision.estimatedReclaimBytes(), analysisBundle.aiProvider(),
			analysisBundle.aiModelName(), decision.aiConfidenceScore(), decision.semanticTags(),
			decision.matchedConditions());
	}

	private void failScan(Long scanJobId, RuntimeException exception) {
		transactionTemplate.executeWithoutResult(status -> findScanJob(scanJobId).markFailed(exception.getMessage()));
	}

	private ScanJob findScanJob(Long scanJobId) {
		return scanJobRepository.findById(scanJobId).orElseThrow(() -> new CustomException(ErrorCode.SCAN_JOB_NOT_FOUND));
	}

	private int countBySource(List<ScannedItem> items, ItemSource itemSource) {
		return toInt(items.stream().filter(item -> item.getItemSource() == itemSource).count());
	}

	private int toInt(long value) {
		return value > Integer.MAX_VALUE ? Integer.MAX_VALUE : (int) value;
	}

	private record ScanExecutionContext(Long userId, String userEmail, ScanCondition condition) {
	}
}
