package com.AURA.AURA_Service.auth.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import java.time.LocalDateTime;

@Entity
@Table(name = "google_permissions")
public class GooglePermission {
	@Id @GeneratedValue(strategy = GenerationType.IDENTITY)
	@Column(name = "permission_id") private Long permissionId;
	@ManyToOne @JoinColumn(name = "user_id", nullable = false) private User user;
	@Enumerated(EnumType.STRING) @Column(name = "service_type", nullable = false) private ServiceType serviceType;
	@Enumerated(EnumType.STRING) @Column(name = "permission_status", nullable = false) private PermissionStatus permissionStatus = PermissionStatus.CONNECTED;
	@Column(name = "scope_text", columnDefinition = "TEXT") private String scopeText;
	@Column(name = "connected_at") private LocalDateTime connectedAt;
	@Column(name = "disconnected_at") private LocalDateTime disconnectedAt;
	@Column(name = "last_checked_at") private LocalDateTime lastCheckedAt;

	protected GooglePermission() { }

	public static GooglePermission create(User user, ServiceType serviceType) {
		GooglePermission permission = new GooglePermission();
		permission.user = user;
		permission.serviceType = serviceType;
		return permission;
	}

	public void connect(String scopeText, LocalDateTime checkedAt) {
		this.permissionStatus = PermissionStatus.CONNECTED;
		this.scopeText = scopeText;
		this.connectedAt = checkedAt;
		this.disconnectedAt = null;
		this.lastCheckedAt = checkedAt;
	}

	public void deny(String scopeText, LocalDateTime checkedAt) {
		this.permissionStatus = PermissionStatus.DENIED;
		this.scopeText = scopeText;
		this.lastCheckedAt = checkedAt;
	}

	public void disconnect(LocalDateTime disconnectedAt) {
		this.permissionStatus = PermissionStatus.DISCONNECTED;
		this.disconnectedAt = disconnectedAt;
		this.lastCheckedAt = disconnectedAt;
	}

	public ServiceType getServiceType() { return serviceType; }
	public PermissionStatus getPermissionStatus() { return permissionStatus; }
	public LocalDateTime getConnectedAt() { return connectedAt; }
	public LocalDateTime getLastCheckedAt() { return lastCheckedAt; }

	public enum ServiceType { GMAIL, DRIVE }
	public enum PermissionStatus { CONNECTED, DENIED, EXPIRED, RECONNECT_REQUIRED, DISCONNECTED }
}
