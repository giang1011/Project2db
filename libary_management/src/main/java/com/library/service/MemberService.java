package com.library.service;

import com.library.model.Member;
import com.library.model.MemberStudentProfile;
import com.library.repository.MemberRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.sql.SQLException;

import com.library.util.UserSession;
import com.library.model.User;

public class MemberService {
    private static final Logger logger = LoggerFactory.getLogger(MemberService.class);
    private final MemberRepository memberRepository;
    private final ActivityLogService activityLogService;

    public MemberService() {
        this.memberRepository = new MemberRepository();
        this.activityLogService = new ActivityLogService();
    }

    // Inject repository if using DI
    public MemberService(MemberRepository memberRepository) {
        this.memberRepository = memberRepository;
        this.activityLogService = new ActivityLogService();
    }

    public void addMember(Member member, MemberStudentProfile studentProfile) throws Exception {
        // Validation logic
        if (member.getMembershipEndDate().isBefore(member.getMembershipStartDate()) || 
            member.getMembershipEndDate().isEqual(member.getMembershipStartDate())) {
            throw new IllegalArgumentException("Ngay ket thuc phai lon hon ngay bat dau.");
        }

        // Apply System Settings
        if ("STUDENT".equals(member.getMemberType())) {
            member.setMaxBorrowBooks(5);
            member.setBorrowDurationDays(30);
            
            if (studentProfile == null || 
                studentProfile.getSchoolName() == null || studentProfile.getSchoolName().isEmpty() ||
                studentProfile.getStudentCode() == null || studentProfile.getStudentCode().isEmpty()) {
                throw new IllegalArgumentException("Thieu thong tin sinh vien (Ten truong, Ma sinh vien).");
            }
        } else if ("NORMAL".equals(member.getMemberType())) {
            member.setMaxBorrowBooks(3);
            member.setBorrowDurationDays(14);
        }

        try {
            memberRepository.saveMember(member, studentProfile);
            long userId = 1;
            User user = UserSession.getInstance().getLoggedInUser();
            if (user != null) userId = user.getUserId();
            activityLogService.logAction(userId, "Add Member", null, "New Member: " + member.getFullName() + " (" + member.getMemberType() + ")");
        } catch (SQLException e) {
            logger.error("Loi khi luu thanh vien vao co so du lieu: ", e);
            throw new Exception("Khong the luu thong tin thanh vien: " + e.getMessage(), e);
        }
    }

    public java.util.List<Member> getAllMembers() throws Exception {
        try {
            return memberRepository.getAllMembers();
        } catch (SQLException e) {
            logger.error("Loi khi lay danh sach doc gia: ", e);
            throw new Exception("Khong the tai du lieu doc gia.", e);
        }
    }
    public void updateMember(Member member) throws Exception {
        try {
            boolean success = memberRepository.updateMember(member);
            if (!success) {
                throw new Exception("Khong the cap nhat thong tin doc gia.");
            }
            long userId = 1;
            User user = UserSession.getInstance().getLoggedInUser();
            if (user != null) userId = user.getUserId();
            activityLogService.logAction(userId, "Update Member", null, "Updated Member: " + member.getFullName());
        } catch (SQLException e) {
            logger.error("Loi khi cap nhat doc gia: ", e);
            throw new Exception("Loi CSDL khi cap nhat: " + e.getMessage(), e);
        }
    }

    public void suspendMember(Long memberId) throws Exception {
        try {
            boolean success = memberRepository.suspendMember(memberId);
            if (!success) {
                throw new Exception("Khong tim thay doc gia de dinh chi.");
            }
            long userId = 1;
            User user = UserSession.getInstance().getLoggedInUser();
            if (user != null) userId = user.getUserId();
            activityLogService.logAction(userId, "Suspend Member", null, "Suspended Member ID: " + memberId);
        } catch (SQLException e) {
            logger.error("Loi khi dinh chi doc gia: ", e);
            throw new Exception("Loi CSDL khi dinh chi: " + e.getMessage(), e);
        }
    }

    public void renewMember(Member member, int months, java.math.BigDecimal amount, Long processedBy) throws Exception {
        try {
            java.time.LocalDate now = java.time.LocalDate.now();
            java.time.LocalDate newEndDate = now.plusMonths(months);
            
            boolean success = memberRepository.renewMember(member.getMemberId(), now, newEndDate, amount, processedBy);
            if (!success) {
                throw new Exception("Gia han that bai.");
            }
            activityLogService.logAction(processedBy, "Renew Member", null, "Renewed Member ID: " + member.getMemberId() + " for " + months + " months");
        } catch (SQLException e) {
            logger.error("Loi khi gia han doc gia: ", e);
            throw new Exception("Loi CSDL khi gia han: " + e.getMessage(), e);
        }
    }

    public int checkAndGetExpiredStudentCount() throws Exception {
        try {
            return memberRepository.updateExpiredMembersAndCountStudent();
        } catch (SQLException e) {
            logger.error("Loi khi cap nhat the het han: ", e);
            throw new Exception("Loi khi quet the het han.", e);
        }
    }

    public void confirmStudentToNormal(Long memberId, Long processedBy) throws Exception {
        try {
            boolean success = memberRepository.confirmStudentToNormal(memberId, processedBy);
            if (!success) {
                throw new Exception("Khong the cap nhat thanh vien sang NORMAL.");
            }
        } catch (SQLException e) {
            logger.error("Loi khi xac nhan the sinh vien sang NORMAL: ", e);
            throw new Exception("Loi CSDL khi xac nhan the: " + e.getMessage(), e);
        }
    }

    public java.util.List<Member> searchMembers(String keyword) throws Exception {
        try {
            return memberRepository.searchMembers(keyword);
        } catch (SQLException e) {
            logger.error("Loi khi tim kiem doc gia: ", e);
            throw new Exception("Loi khi tim kiem doc gia.", e);
        }
    }
}
