
CREATE DATABASE PublicLibraryDB;
GO

USE PublicLibraryDB;
GO

-- =====================================================
-- USERS
-- =====================================================

CREATE TABLE Users (
    UserID BIGINT PRIMARY KEY IDENTITY(1,1),

    Username VARCHAR(50)
        NOT NULL UNIQUE,

    PasswordHash VARCHAR(255)
        NOT NULL,

    FullName NVARCHAR(150)
        NOT NULL,

    Email VARCHAR(150)
        UNIQUE,

    Role VARCHAR(20)
        NOT NULL
        CHECK (Role IN ('ADMIN', 'LIBRARIAN')),

    Status VARCHAR(20)
        DEFAULT 'ACTIVE'
        CHECK (Status IN ('ACTIVE', 'INACTIVE')),

    CreatedAt DATETIME2
        DEFAULT SYSDATETIME(),

    UpdatedAt DATETIME2
        DEFAULT SYSDATETIME()
);
GO

-- =====================================================
-- CATEGORIES
-- =====================================================

CREATE TABLE Categories (
    CategoryID INT PRIMARY KEY IDENTITY(1,1),

    CategoryName NVARCHAR(100)
        NOT NULL UNIQUE,

    Description NVARCHAR(MAX)
);
GO

-- =====================================================
-- PUBLISHERS
-- =====================================================

CREATE TABLE Publishers (
    PublisherID INT PRIMARY KEY IDENTITY(1,1),

    PublisherName NVARCHAR(150)
        NOT NULL UNIQUE,

    Address NVARCHAR(MAX),

    Phone VARCHAR(20),

    Email VARCHAR(150)
);
GO

-- =====================================================
-- AUTHORS
-- =====================================================

CREATE TABLE Authors (
    AuthorID INT PRIMARY KEY IDENTITY(1,1),

    AuthorName NVARCHAR(150)
        NOT NULL,

    Biography NVARCHAR(MAX)
);
GO

-- =====================================================
-- BOOKS
-- =====================================================

CREATE TABLE Books (
    BookID BIGINT PRIMARY KEY IDENTITY(1,1),

    ISBN VARCHAR(20)
        UNIQUE,

    Title NVARCHAR(255)
        NOT NULL,

    PublisherID INT,

    PublicationYear INT,

    Language NVARCHAR(50),

    Description NVARCHAR(MAX),

    PageCount INT
        CHECK (PageCount > 0),

    CoverImage VARCHAR(255),

    IsDeleted BIT
        DEFAULT 0,

    CreatedBy BIGINT NULL,

    UpdatedBy BIGINT NULL,

    CreatedAt DATETIME2
        DEFAULT SYSDATETIME(),

    UpdatedAt DATETIME2
        DEFAULT SYSDATETIME(),

    FOREIGN KEY (PublisherID)
        REFERENCES Publishers(PublisherID)
        ON DELETE SET NULL,

    FOREIGN KEY (CreatedBy)
        REFERENCES Users(UserID)
        ON DELETE SET NULL,

    FOREIGN KEY (UpdatedBy)
        REFERENCES Users(UserID)
);
GO

-- =====================================================
-- BOOK AUTHORS
-- =====================================================

CREATE TABLE BookAuthors (
    BookID BIGINT NOT NULL,

    AuthorID INT NOT NULL,

    PRIMARY KEY (BookID, AuthorID),

    FOREIGN KEY (BookID)
        REFERENCES Books(BookID)
        ON DELETE CASCADE,

    FOREIGN KEY (AuthorID)
        REFERENCES Authors(AuthorID)
        ON DELETE CASCADE
);
GO

-- =====================================================
-- BOOK CATEGORIES
-- =====================================================

CREATE TABLE BookCategories (
    BookID BIGINT NOT NULL,

    CategoryID INT NOT NULL,

    PRIMARY KEY (BookID, CategoryID),

    FOREIGN KEY (BookID)
        REFERENCES Books(BookID)
        ON DELETE CASCADE,

    FOREIGN KEY (CategoryID)
        REFERENCES Categories(CategoryID)
        ON DELETE CASCADE
);
GO

-- =====================================================
-- BOOK COPIES
-- =====================================================

CREATE TABLE BookCopies (
    CopyID BIGINT PRIMARY KEY IDENTITY(1,1),

    BookID BIGINT NOT NULL,

    Barcode VARCHAR(100)
        NOT NULL UNIQUE,

    ShelfLocation NVARCHAR(100),

    AcquisitionDate DATE,

    PhysicalCondition VARCHAR(20)
        DEFAULT 'GOOD'
        CHECK (
            PhysicalCondition IN (
                'GOOD',
                'DAMAGED',
                'LOST'
            )
        ),

    DamageDescription NVARCHAR(MAX),

    CirculationStatus VARCHAR(20)
        DEFAULT 'AVAILABLE'
        CHECK (
            CirculationStatus IN (
                'AVAILABLE',
                'BORROWED',
                'LOST'
            )
        ),

    IsReferenceOnly BIT
        DEFAULT 0,

    IsDeleted BIT
        DEFAULT 0,

    LastInventoryCheck DATE,

    CreatedAt DATETIME2
        DEFAULT SYSDATETIME(),

    FOREIGN KEY (BookID)
        REFERENCES Books(BookID)
        ON DELETE NO ACTION
);
GO

-- =====================================================
-- MEMBERS
-- =====================================================

CREATE TABLE Members (
    MemberID BIGINT PRIMARY KEY IDENTITY(1,1),

    MemberCode VARCHAR(50)
        NOT NULL UNIQUE,

    FullName NVARCHAR(150)
        NOT NULL,

    Email VARCHAR(150)
        UNIQUE,

    Phone VARCHAR(20)
        UNIQUE,

    MemberType VARCHAR(20)
        NOT NULL
        CHECK (
            MemberType IN (
                'STUDENT',
                'NORMAL'
            )
        ),

    DateOfBirth DATE,

    Address NVARCHAR(MAX),

    MembershipStartDate DATE
        NOT NULL,

    MembershipEndDate DATE
        NOT NULL,

    MaxBorrowBooks INT
        NOT NULL,

    BorrowDurationDays INT
        NOT NULL,

    Status VARCHAR(20)
        DEFAULT 'ACTIVE'
        CHECK (
            Status IN (
                'ACTIVE',
                'EXPIRED',
                'SUSPENDED'
            )
        ),

    CreatedBy BIGINT NULL,

    UpdatedBy BIGINT NULL,

    CreatedAt DATETIME2
        DEFAULT SYSDATETIME(),

    UpdatedAt DATETIME2
        DEFAULT SYSDATETIME(),

    CHECK (
        MembershipEndDate > MembershipStartDate
    ),

    FOREIGN KEY (CreatedBy)
        REFERENCES Users(UserID)
        ON DELETE SET NULL,

    FOREIGN KEY (UpdatedBy)
        REFERENCES Users(UserID)
);
GO

-- =====================================================
-- MEMBER STUDENT PROFILES
-- =====================================================

CREATE TABLE MemberStudentProfiles (
    StudentProfileID BIGINT PRIMARY KEY IDENTITY(1,1),

    MemberID BIGINT
        NOT NULL UNIQUE,

    SchoolName NVARCHAR(255)
        NOT NULL,

    StudentCode VARCHAR(100)
        NOT NULL UNIQUE,

    StudentStatus VARCHAR(20)
        DEFAULT 'ACTIVE'
        CHECK (
            StudentStatus IN (
                'ACTIVE',
                'GRADUATED',
                'EXPIRED'
            )
        ),

    StudentVerificationStatus VARCHAR(20)
        DEFAULT 'PENDING'
        CHECK (
            StudentVerificationStatus IN (
                'PENDING',
                'VERIFIED',
                'REJECTED'
            )
        ),

    FOREIGN KEY (MemberID)
        REFERENCES Members(MemberID)
        ON DELETE CASCADE
);
GO

-- =====================================================
-- BORROW TRANSACTIONS
-- =====================================================

CREATE TABLE BorrowTransactions (
    TransactionID BIGINT PRIMARY KEY IDENTITY(1,1),

    MemberID BIGINT NOT NULL,

    BorrowedBy BIGINT NOT NULL,

    BorrowDate DATETIME2
        DEFAULT SYSDATETIME(),

    Notes NVARCHAR(MAX),

    FOREIGN KEY (MemberID)
        REFERENCES Members(MemberID)
        ON DELETE NO ACTION,

    FOREIGN KEY (BorrowedBy)
        REFERENCES Users(UserID)
        ON DELETE NO ACTION
);
GO

-- =====================================================
-- BORROW ITEMS
-- =====================================================

CREATE TABLE BorrowItems (
    BorrowItemID BIGINT PRIMARY KEY IDENTITY(1,1),

    TransactionID BIGINT NOT NULL,

    CopyID BIGINT NOT NULL,

    DueDate DATE NOT NULL,

    ReturnDate DATE,

    RenewalCount INT
        DEFAULT 0
        CHECK (RenewalCount >= 0),

    Status VARCHAR(20)
        DEFAULT 'BORROWING'
        CHECK (
            Status IN (
                'BORROWING',
                'RETURNED',
                'OVERDUE',
                'LOST'
            )
        ),

    FOREIGN KEY (TransactionID)
        REFERENCES BorrowTransactions(TransactionID)
        ON DELETE NO ACTION,

    FOREIGN KEY (CopyID)
        REFERENCES BookCopies(CopyID)
        ON DELETE NO ACTION
);
GO

-- =====================================================
-- RETURN TRANSACTIONS
-- =====================================================

CREATE TABLE ReturnTransactions (
    ReturnTransactionID BIGINT PRIMARY KEY IDENTITY(1,1),

    MemberID BIGINT NOT NULL,

    ProcessedBy BIGINT NOT NULL,

    ReturnDate DATETIME2
        DEFAULT SYSDATETIME(),

    Notes NVARCHAR(MAX),

    FOREIGN KEY (MemberID)
        REFERENCES Members(MemberID)
        ON DELETE NO ACTION,

    FOREIGN KEY (ProcessedBy)
        REFERENCES Users(UserID)
        ON DELETE NO ACTION
);
GO

-- =====================================================
-- RETURN ITEMS
-- =====================================================

CREATE TABLE ReturnItems (
    ReturnItemID BIGINT PRIMARY KEY IDENTITY(1,1),

    ReturnTransactionID BIGINT NOT NULL,

    BorrowItemID BIGINT NOT NULL,

    ReturnedCondition VARCHAR(20)
        DEFAULT 'GOOD'
        CHECK (
            ReturnedCondition IN (
                'GOOD',
                'DAMAGED',
                'LOST'
            )
        ),

    Notes NVARCHAR(MAX),

    FOREIGN KEY (ReturnTransactionID)
        REFERENCES ReturnTransactions(ReturnTransactionID)
        ON DELETE NO ACTION,

    FOREIGN KEY (BorrowItemID)
        REFERENCES BorrowItems(BorrowItemID)
        ON DELETE NO ACTION
);
GO

-- =====================================================
-- FINES
-- =====================================================

CREATE TABLE Fines (
    FineID BIGINT PRIMARY KEY IDENTITY(1,1),

    MemberID BIGINT NOT NULL,

    BorrowItemID BIGINT NULL,

    FineType VARCHAR(20)
        NOT NULL
        CHECK (
            FineType IN (
                'OVERDUE',
                'DAMAGED',
                'LOST'
            )
        ),

    Amount DECIMAL(10,0)
        NOT NULL
        CHECK (Amount >= 0),

    PaidAmount DECIMAL(10,0)
        DEFAULT 0
        CHECK (PaidAmount >= 0),

    Status VARCHAR(20)
        DEFAULT 'UNPAID'
        CHECK (
            Status IN (
                'UNPAID',
                'PARTIAL',
                'PAID'
            )
        ),

    IssuedAt DATETIME2
        DEFAULT SYSDATETIME(),

    PaidAt DATETIME2 NULL,

    Notes NVARCHAR(MAX),

    FOREIGN KEY (MemberID)
        REFERENCES Members(MemberID)
        ON DELETE NO ACTION,

    FOREIGN KEY (BorrowItemID)
        REFERENCES BorrowItems(BorrowItemID)
        ON DELETE SET NULL
);
GO

-- =====================================================
-- NOTIFICATIONS
-- =====================================================

CREATE TABLE Notifications (
    NotificationID BIGINT PRIMARY KEY IDENTITY(1,1),

    MemberID BIGINT NOT NULL,

    Title NVARCHAR(255)
        NOT NULL,

    Message NVARCHAR(MAX)
        NOT NULL,

    IsRead BIT
        DEFAULT 0,

    CreatedAt DATETIME2
        DEFAULT SYSDATETIME(),

    FOREIGN KEY (MemberID)
        REFERENCES Members(MemberID)
        ON DELETE CASCADE
);
GO

-- =====================================================
-- MEMBERSHIP PAYMENTS
-- =====================================================

CREATE TABLE MembershipPayments (
    MembershipPaymentID BIGINT PRIMARY KEY IDENTITY(1,1),

    MemberID BIGINT NOT NULL,

    Amount DECIMAL(10,0)
        NOT NULL
        CHECK (Amount >= 0),

    PaymentType VARCHAR(20)
        NOT NULL
        CHECK (
            PaymentType IN (
                'NEW_CARD',
                'RENEWAL'
            )
        ),

    PaymentMethod VARCHAR(20)
        DEFAULT 'CASH'
        CHECK (
            PaymentMethod IN (
                'CASH',
                'BANK_TRANSFER',
                'FREE'
            )
        ),

    PaidAt DATETIME2
        DEFAULT SYSDATETIME(),

    ProcessedBy BIGINT NOT NULL,

    FOREIGN KEY (MemberID)
        REFERENCES Members(MemberID)
        ON DELETE NO ACTION,

    FOREIGN KEY (ProcessedBy)
        REFERENCES Users(UserID)
        ON DELETE NO ACTION
);
GO

-- =====================================================
-- MEMBERSHIP LOGS
-- =====================================================

CREATE TABLE MembershipLogs (
    LogID BIGINT PRIMARY KEY IDENTITY(1,1),

    MemberID BIGINT NOT NULL,

    OldMemberType VARCHAR(20)
        CHECK (
            OldMemberType IN (
                'STUDENT',
                'NORMAL'
            )
        ),

    NewMemberType VARCHAR(20)
        CHECK (
            NewMemberType IN (
                'STUDENT',
                'NORMAL'
            )
        ),

    ActionType VARCHAR(20)
        NOT NULL
        CHECK (
            ActionType IN (
                'NEW',
                'RENEWAL',
                'UPGRADE',
                'DOWNGRADE',
                'EXPIRE'
            )
        ),

    ChangedAt DATETIME2
        DEFAULT SYSDATETIME(),

    ProcessedBy BIGINT NULL,

    Notes NVARCHAR(MAX),

    FOREIGN KEY (MemberID)
        REFERENCES Members(MemberID)
        ON DELETE NO ACTION,

    FOREIGN KEY (ProcessedBy)
        REFERENCES Users(UserID)
        ON DELETE SET NULL
);
GO

-- =====================================================
-- SYSTEM SETTINGS
-- =====================================================

CREATE TABLE SystemSettings (
    SettingID BIGINT PRIMARY KEY IDENTITY(1,1),

    SettingKey VARCHAR(100)
        NOT NULL UNIQUE,

    SettingValue VARCHAR(255)
        NOT NULL,

    DataType VARCHAR(20)
        NOT NULL
        CHECK (
            DataType IN (
                'STRING',
                'INTEGER',
                'DECIMAL',
                'BOOLEAN'
            )
        ),

    ValidationRegex VARCHAR(255),

    Description NVARCHAR(MAX),

    UpdatedAt DATETIME2
        DEFAULT SYSDATETIME()
);
GO

-- =====================================================
-- ACTIVITY LOGS
-- =====================================================

CREATE TABLE ActivityLogs (
    LogID BIGINT PRIMARY KEY IDENTITY(1,1),

    UserID BIGINT NOT NULL,

    Action NVARCHAR(255)
        NOT NULL,

    OldValue NVARCHAR(MAX),

    NewValue NVARCHAR(MAX),

    CreatedAt DATETIME2
        DEFAULT SYSDATETIME(),

    FOREIGN KEY (UserID)
        REFERENCES Users(UserID)
        ON DELETE NO ACTION
);
GO

-- =====================================================
-- INDEXES
-- =====================================================
CREATE INDEX idx_member_expiry 
ON Members(Status, MembershipEndDate);
GO


CREATE INDEX idx_books_titlegiang101
ON Books(Title);
GO

CREATE INDEX idx_books_isbn
ON Books(ISBN);
GO

CREATE INDEX idx_books_publisher
ON Books(PublisherID);
GO

CREATE INDEX idx_member_name
ON Members(FullName);
GO

CREATE INDEX idx_member_code
ON Members(MemberCode);
GO

CREATE INDEX idx_bookcopies_barcode
ON BookCopies(Barcode);
GO

CREATE INDEX idx_borrow_member
ON BorrowTransactions(MemberID);
GO

CREATE INDEX idx_borrow_due
ON BorrowItems(Status, DueDate);
GO

CREATE INDEX idx_fines_member
ON Fines(MemberID);
GO

CREATE INDEX idx_notifications_read
ON Notifications(MemberID, IsRead);
GO

-- =====================================================
-- DEFAULT SYSTEM SETTINGS
-- =====================================================

INSERT INTO SystemSettings
(
    SettingKey,
    SettingValue,
    DataType,
    Description
)
VALUES
('MAX_BORROW_BOOKS_STUDENT', '5', 'INTEGER', N'Maximum books a student can borrow'),

('MAX_BORROW_BOOKS_NORMAL', '3', 'INTEGER', N'Maximum books a normal member can borrow'),

('BORROW_DURATION_STUDENT', '30', 'INTEGER', N'Borrowing duration for students (days)'),

('BORROW_DURATION_NORMAL', '14', 'INTEGER', N'Borrowing duration for normal members (days)'),

('MAX_RENEWAL_COUNT', '2', 'INTEGER', N'Maximum number of renewals'),

('OVERDUE_FINE_PER_DAY', '5000', 'DECIMAL', N'Overdue fine per day'),

('LOST_BOOK_FINE_MULTIPLIER', '2', 'DECIMAL', N'Lost book fine multiplier (relative to book price)'),

('MEMBERSHIP_FEE_STUDENT', '50000', 'DECIMAL', N'Membership fee for students'),

('MEMBERSHIP_FEE_NORMAL', '100000', 'DECIMAL', N'Membership fee for normal members'),

('FREE_STUDENT_CARD', 'TRUE', 'BOOLEAN', N'Enable free student card issuance');
GO


