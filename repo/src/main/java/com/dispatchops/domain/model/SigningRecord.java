package com.dispatchops.domain.model;

import java.time.LocalDateTime;

public class SigningRecord {

    private Long id;
    private Long contractInstanceId;
    private Long signerId;
    private int signerOrder;
    private String signatureData;
    private String documentHash;
    private LocalDateTime signedAt;
    private String ipAddress;

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Long getContractInstanceId() {
        return contractInstanceId;
    }

    public void setContractInstanceId(Long contractInstanceId) {
        this.contractInstanceId = contractInstanceId;
    }

    public Long getSignerId() {
        return signerId;
    }

    public void setSignerId(Long signerId) {
        this.signerId = signerId;
    }

    public int getSignerOrder() {
        return signerOrder;
    }

    public void setSignerOrder(int signerOrder) {
        this.signerOrder = signerOrder;
    }

    public String getSignatureData() {
        return signatureData;
    }

    public void setSignatureData(String signatureData) {
        this.signatureData = signatureData;
    }

    public String getDocumentHash() {
        return documentHash;
    }

    public void setDocumentHash(String documentHash) {
        this.documentHash = documentHash;
    }

    public LocalDateTime getSignedAt() {
        return signedAt;
    }

    public void setSignedAt(LocalDateTime signedAt) {
        this.signedAt = signedAt;
    }

    public String getIpAddress() {
        return ipAddress;
    }

    public void setIpAddress(String ipAddress) {
        this.ipAddress = ipAddress;
    }
}
