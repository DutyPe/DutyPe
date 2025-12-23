# Requirements Document

## Introduction

This document specifies the requirements for a set of bug fixes and enhancements to the DutyPe Android application. The changes address permission handling flow, profile setup screen issues, date picker UI improvements, login validation, Firebase user verification, empty state navigation, and comprehensive logging throughout the application.

## Glossary

- **DutyPe_System**: The DutyPe Android mobile application for connecting workers with employers
- **Onboarding_Screen**: The initial tutorial screens shown to new users
- **Select_Role_Screen**: The screen where users choose between Worker and Employer roles
- **Worker_Home_Screen**: The main dashboard for worker users
- **Employer_Home_Screen**: The main dashboard for employer users
- **Mandatory_Profile_Setup_Screen**: The screen where users complete their profile information
- **Firebase_Database**: The cloud database storing user profiles and authentication data
- **Indian_Phone_Number**: A 10-digit phone number valid in India (starting with 6, 7, 8, or 9)
- **Permission_Request**: System dialog requesting user consent for notifications or location access
- **Date_Picker**: A calendar-style UI component for selecting dates
- **Empty_State**: UI displayed when a list or collection has no items

## Requirements

### Requirement 1

**User Story:** As a new user, I want to be asked for notification and location permissions after completing onboarding and before selecting my role, so that the permission flow is streamlined and not disruptive during my main app usage.

#### Acceptance Criteria

1. WHEN a user completes the Onboarding_Screen and navigates to Select_Role_Screen THEN the DutyPe_System SHALL request notification permission from the user
2. WHEN a user completes the Onboarding_Screen and navigates to Select_Role_Screen THEN the DutyPe_System SHALL request location permission from the user
3. WHEN the Select_Role_Screen loads THEN the DutyPe_System SHALL display permission requests before showing role selection options
4. WHEN the Worker_Home_Screen loads THEN the DutyPe_System SHALL NOT request notification or location permissions
5. WHEN the Employer_Home_Screen loads THEN the DutyPe_System SHALL NOT request notification or location permissions

### Requirement 2

**User Story:** As a user completing my profile, I want the fetch button to work correctly and the Next button to be consistently sized and readable, so that I can complete my profile setup without confusion.

#### Acceptance Criteria

1. WHEN a user taps the fetch button on Mandatory_Profile_Setup_Screen THEN the DutyPe_System SHALL retrieve and populate the relevant field data
2. WHEN the Mandatory_Profile_Setup_Screen displays the Next button THEN the DutyPe_System SHALL render the button with sufficient width to display "Next" text completely
3. WHEN the Mandatory_Profile_Setup_Screen displays the Complete Profile button THEN the DutyPe_System SHALL render the button with sufficient width to display "Complete Profile" text completely
4. WHEN the user is on any step of Mandatory_Profile_Setup_Screen THEN the DutyPe_System SHALL maintain consistent button sizing across all steps

### Requirement 3

**User Story:** As a user entering my date of birth, I want to see a modern calendar-style date picker with month/year header and day grid, so that I can easily select my birth date.

#### Acceptance Criteria

1. WHEN a user taps the date of birth field THEN the DutyPe_System SHALL display a calendar-style date picker dialog
2. WHEN the date picker displays THEN the DutyPe_System SHALL show a header with the selected year and formatted date (e.g., "1990" and "Mon, Jan 1")
3. WHEN the date picker displays THEN the DutyPe_System SHALL show month navigation arrows to move between months
4. WHEN the date picker displays THEN the DutyPe_System SHALL show a grid of days with weekday headers (S, M, T, W, T, F, S)
5. WHEN a user selects a date THEN the DutyPe_System SHALL highlight the selected date with a circular indicator
6. WHEN a user taps OK on the date picker THEN the DutyPe_System SHALL populate the date of birth field with the selected date
7. WHEN a user taps CANCEL on the date picker THEN the DutyPe_System SHALL close the dialog without changing the date of birth field

### Requirement 4

**User Story:** As a user entering my phone number on the login screen, I want the Continue button to activate only for valid Indian phone numbers and see clear error messages for invalid numbers, so that I understand what input is expected.

#### Acceptance Criteria

1. WHEN a user enters a phone number that does not start with 6, 7, 8, or 9 THEN the DutyPe_System SHALL keep the Continue button disabled
2. WHEN a user enters a phone number with fewer than 10 digits THEN the DutyPe_System SHALL keep the Continue button disabled
3. WHEN a user enters a valid 10-digit Indian phone number starting with 6, 7, 8, or 9 THEN the DutyPe_System SHALL enable the Continue button
4. WHEN a user enters an invalid phone number and the input field loses focus THEN the DutyPe_System SHALL display an error message indicating the phone number format is invalid
5. WHEN a user enters a phone number starting with digits 0-5 THEN the DutyPe_System SHALL display an error message stating "Phone number must start with 6, 7, 8, or 9"

### Requirement 5

**User Story:** As a returning user, I want the login screen to check if my phone number exists in Firebase and if I have profile data, so that I am directed to the appropriate screen without re-entering my profile information.

#### Acceptance Criteria

1. WHEN a user successfully verifies OTP THEN the DutyPe_System SHALL query Firebase_Database to check if the phone number exists
2. WHEN the phone number exists in Firebase_Database with complete profile data THEN the DutyPe_System SHALL navigate the user directly to the appropriate home screen (Worker_Home_Screen or Employer_Home_Screen)
3. WHEN the phone number exists in Firebase_Database without complete profile data THEN the DutyPe_System SHALL navigate the user to Mandatory_Profile_Setup_Screen
4. WHEN the phone number does not exist in Firebase_Database THEN the DutyPe_System SHALL navigate the user to Mandatory_Profile_Setup_Screen as a new user
5. WHEN checking user existence THEN the DutyPe_System SHALL log the database query and result for debugging purposes

### Requirement 6

**User Story:** As a user viewing empty saved jobs or applied jobs lists, I want the "Browse Jobs" or "Find Jobs" button to navigate me to the worker home screen, so that I can discover available jobs.

#### Acceptance Criteria

1. WHEN a user taps "Browse Available Jobs" button on the empty Saved Jobs state THEN the DutyPe_System SHALL navigate to Worker_Home_Screen
2. WHEN a user taps "Find Jobs" button on the empty Applied Jobs state THEN the DutyPe_System SHALL navigate to Worker_Home_Screen
3. WHEN navigation from empty state occurs THEN the DutyPe_System SHALL log the navigation event for debugging purposes

### Requirement 7

**User Story:** As a developer, I want comprehensive logging throughout the application for navigation, permissions, profile checking, and user verification, so that I can debug issues effectively.

#### Acceptance Criteria

1. WHEN any screen navigation occurs THEN the DutyPe_System SHALL log the source screen, destination screen, and any navigation parameters
2. WHEN permission requests are made THEN the DutyPe_System SHALL log the permission type, request status, and user response
3. WHEN profile data is checked or loaded THEN the DutyPe_System SHALL log the user identifier, data fields checked, and result
4. WHEN user phone number existence is verified THEN the DutyPe_System SHALL log the phone number (masked), query status, and result
5. WHEN Firebase_Database operations occur THEN the DutyPe_System SHALL log the operation type, collection/document path, and success/failure status
6. WHEN authentication state changes THEN the DutyPe_System SHALL log the previous state, new state, and authentication method used
