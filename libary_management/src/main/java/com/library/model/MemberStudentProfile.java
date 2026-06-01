package com.library.model;

public class MemberStudentProfile {
    private Long studentProfileId;
    private Long memberId;
    private String schoolName;
    private String studentCode;
    private String studentStatus;
    private String studentVerificationStatus;

    public MemberStudentProfile() {
    }

    public Long getStudentProfileId() {
        return studentProfileId;
    }

    public void setStudentProfileId(Long studentProfileId) {
        this.studentProfileId = studentProfileId;
    }

    public Long getMemberId() {
        return memberId;
    }

    public void setMemberId(Long memberId) {
        this.memberId = memberId;
    }

    public String getSchoolName() {
        return schoolName;
    }

    public void setSchoolName(String schoolName) {
        this.schoolName = schoolName;
    }

    public String getStudentCode() {
        return studentCode;
    }

    public void setStudentCode(String studentCode) {
        this.studentCode = studentCode;
    }

    public String getStudentStatus() {
        return studentStatus;
    }

    public void setStudentStatus(String studentStatus) {
        this.studentStatus = studentStatus;
    }

    public String getStudentVerificationStatus() {
        return studentVerificationStatus;
    }

    public void setStudentVerificationStatus(String studentVerificationStatus) {
        this.studentVerificationStatus = studentVerificationStatus;
    }
}
