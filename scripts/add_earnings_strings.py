#!/usr/bin/env python3
"""Add earnings/payment related strings to English and Telugu strings.xml."""
import xml.etree.ElementTree as ET
import os

BASE = os.path.join(os.path.dirname(__file__), '..', 'app', 'src', 'main', 'res')

EN_STRINGS = [
    # EarningsDashboardScreen
    ("recent_transactions", "Recent Transactions"),
    ("view_all", "View All"),
    ("total_earnings_label", "Total Earnings"),
    ("pending_label", "Pending"),
    ("weekly_earnings", "Weekly Earnings"),
    # EarningsPeriod enum
    ("period_this_week", "This Week"),
    ("period_this_month", "This Month"),
    ("period_last_month", "Last Month"),
    ("period_3_months", "3 Months"),
    ("period_all_time", "All Time"),
    # PaymentStatus enum
    ("status_paid", "Paid"),
    ("status_pending", "Pending"),
    ("status_failed", "Failed"),
    # WorkerProfile login subtitles
    ("login_to_view_profile", "Login to view and edit your profile"),
    ("login_to_view_applications", "Login to view your job applications"),
    ("login_to_view_earnings", "Login to view your earnings"),
    ("login_to_create_visiting_card", "Login to create your digital visiting card"),
    ("login_to_refer_earn", "Login to refer friends and earn rewards"),
    ("login_to_access_feature", "Please login to access this feature"),
    # EmployerApplicationManagementScreen
    ("contact_unlocked", "Contact unlocked! \u2705"),
    ("payment_failed_error", "Payment failed: %1$s"),
    ("unlock_contact", "Unlock Contact"),
    ("unlock_worker_contact_desc", "Unlock %1$s\\'s contact details to reach out directly."),
    ("unlock_price", "Unlock Price"),
    ("benefit_phone_number", "\U0001F4DE Get phone number instantly"),
    ("benefit_direct_communication", "\U0001F4AC Direct communication"),
    ("benefit_faster_hiring", "\u26A1 Faster hiring process"),
    ("pay_and_unlock", "Pay \u20B9%1$d &amp; Unlock"),
    # EditJobScreen
    ("payment_details", "Payment Details"),
]

TE_STRINGS = [
    # EarningsDashboardScreen
    ("recent_transactions", "\u0c07\u0c1f\u0c40\u0c35\u0c32 \u0c32\u0c3e\u0c35\u0c26\u0c47\u0c35\u0c40\u0c32\u0c41"),
    ("view_all", "\u0c05\u0c28\u0c4d\u0c28\u0c40 \u0c1a\u0c42\u0c21\u0c02\u0c21\u0c3f"),
    ("total_earnings_label", "\u0c2e\u0c4a\u0c24\u0c4d\u0c24\u0c02 \u0c38\u0c02\u0c2a\u0c3e\u0c26\u0c28"),
    ("pending_label", "\u0c2a\u0c46\u0c02\u0c21\u0c3f\u0c02\u0c17\u0c4d"),
    ("weekly_earnings", "\u0c35\u0c3e\u0c30\u0c2a\u0c41 \u0c38\u0c02\u0c2a\u0c3e\u0c26\u0c28"),
    # EarningsPeriod enum
    ("period_this_week", "\u0c08 \u0c35\u0c3e\u0c30\u0c02"),
    ("period_this_month", "\u0c08 \u0c28\u0c46\u0c32"),
    ("period_last_month", "\u0c17\u0c24 \u0c28\u0c46\u0c32"),
    ("period_3_months", "3 \u0c28\u0c46\u0c32\u0c32\u0c41"),
    ("period_all_time", "\u0c05\u0c28\u0c4d\u0c28\u0c3f \u0c38\u0c2e\u0c2f\u0c3e\u0c32\u0c41"),
    # PaymentStatus enum
    ("status_paid", "\u0c1a\u0c46\u0c32\u0c4d\u0c32\u0c3f\u0c02\u0c1a\u0c2c\u0c21\u0c3f\u0c02\u0c26\u0c3f"),
    ("status_pending", "\u0c2a\u0c46\u0c02\u0c21\u0c3f\u0c02\u0c17\u0c4d"),
    ("status_failed", "\u0c35\u0c3f\u0c2b\u0c32\u0c2e\u0c48\u0c02\u0c26\u0c3f"),
    # WorkerProfile login subtitles
    ("login_to_view_profile", "\u0c2e\u0c40 \u0c2a\u0c4d\u0c30\u0c4a\u0c2b\u0c48\u0c32\u0c4d\u200c\u0c28\u0c41 \u0c1a\u0c42\u0c21\u0c21\u0c3e\u0c28\u0c3f\u0c15\u0c3f \u0c2e\u0c30\u0c3f\u0c2f\u0c41 \u0c0e\u0c21\u0c3f\u0c1f\u0c4d \u0c1a\u0c47\u0c2f\u0c21\u0c3e\u0c28\u0c3f\u0c15\u0c3f \u0c32\u0c3e\u0c17\u0c3f\u0c28\u0c4d \u0c05\u0c35\u0c4d\u0c35\u0c02\u0c21\u0c3f"),
    ("login_to_view_applications", "\u0c2e\u0c40 \u0c09\u0c26\u0c4d\u0c2f\u0c4b\u0c17 \u0c26\u0c30\u0c16\u0c3e\u0c38\u0c4d\u0c24\u0c41\u0c32\u0c28\u0c41 \u0c1a\u0c42\u0c21\u0c21\u0c3e\u0c28\u0c3f\u0c15\u0c3f \u0c32\u0c3e\u0c17\u0c3f\u0c28\u0c4d \u0c05\u0c35\u0c4d\u0c35\u0c02\u0c21\u0c3f"),
    ("login_to_view_earnings", "\u0c2e\u0c40 \u0c38\u0c02\u0c2a\u0c3e\u0c26\u0c28\u0c28\u0c41 \u0c1a\u0c42\u0c21\u0c21\u0c3e\u0c28\u0c3f\u0c15\u0c3f \u0c32\u0c3e\u0c17\u0c3f\u0c28\u0c4d \u0c05\u0c35\u0c4d\u0c35\u0c02\u0c21\u0c3f"),
    ("login_to_create_visiting_card", "\u0c2e\u0c40 \u0c21\u0c3f\u0c1c\u0c3f\u0c1f\u0c32\u0c4d \u0c35\u0c3f\u0c1c\u0c3f\u0c1f\u0c3f\u0c02\u0c17\u0c4d \u0c15\u0c3e\u0c30\u0c4d\u0c21\u0c4d \u0c30\u0c42\u0c2a\u0c4a\u0c02\u0c26\u0c3f\u0c02\u0c1a\u0c21\u0c3e\u0c28\u0c3f\u0c15\u0c3f \u0c32\u0c3e\u0c17\u0c3f\u0c28\u0c4d \u0c05\u0c35\u0c4d\u0c35\u0c02\u0c21\u0c3f"),
    ("login_to_refer_earn", "\u0c38\u0c4d\u0c28\u0c47\u0c39\u0c3f\u0c24\u0c41\u0c32\u0c28\u0c41 \u0c30\u0c3f\u0c2b\u0c30\u0c4d \u0c1a\u0c47\u0c38\u0c3f \u0c30\u0c3f\u0c35\u0c3e\u0c30\u0c4d\u0c21\u0c4d\u200c\u0c32\u0c41 \u0c38\u0c02\u0c2a\u0c3e\u0c26\u0c3f\u0c02\u0c1a\u0c21\u0c3e\u0c28\u0c3f\u0c15\u0c3f \u0c32\u0c3e\u0c17\u0c3f\u0c28\u0c4d \u0c05\u0c35\u0c4d\u0c35\u0c02\u0c21\u0c3f"),
    ("login_to_access_feature", "\u0c08 \u0c2b\u0c40\u0c1a\u0c30\u0c4d\u200c\u0c28\u0c41 \u0c2f\u0c3e\u0c15\u0c4d\u0c38\u0c46\u0c38\u0c4d \u0c1a\u0c47\u0c2f\u0c21\u0c3e\u0c28\u0c3f\u0c15\u0c3f \u0c26\u0c2f\u0c1a\u0c47\u0c38\u0c3f \u0c32\u0c3e\u0c17\u0c3f\u0c28\u0c4d \u0c05\u0c35\u0c4d\u0c35\u0c02\u0c21\u0c3f"),
    # EmployerApplicationManagementScreen
    ("contact_unlocked", "\u0c38\u0c02\u0c2a\u0c4d\u0c30\u0c26\u0c3f\u0c02\u0c2a\u0c41 \u0c05\u0c28\u0c4d\u200c\u0c32\u0c3e\u0c15\u0c4d \u0c1a\u0c47\u0c2f\u0c2c\u0c21\u0c3f\u0c02\u0c26\u0c3f! \u2705"),
    ("payment_failed_error", "\u0c1a\u0c46\u0c32\u0c4d\u0c32\u0c3f\u0c02\u0c2a\u0c41 \u0c35\u0c3f\u0c2b\u0c32\u0c2e\u0c48\u0c02\u0c26\u0c3f: %1$s"),
    ("unlock_contact", "\u0c38\u0c02\u0c2a\u0c4d\u0c30\u0c26\u0c3f\u0c02\u0c2a\u0c41 \u0c05\u0c28\u0c4d\u200c\u0c32\u0c3e\u0c15\u0c4d \u0c1a\u0c47\u0c2f\u0c02\u0c21\u0c3f"),
    ("unlock_worker_contact_desc", "%1$s \u0c38\u0c02\u0c2a\u0c4d\u0c30\u0c26\u0c3f\u0c02\u0c2a\u0c41 \u0c35\u0c3f\u0c35\u0c30\u0c3e\u0c32\u0c28\u0c41 \u0c28\u0c47\u0c30\u0c41\u0c17\u0c3e \u0c38\u0c02\u0c2a\u0c4d\u0c30\u0c26\u0c3f\u0c02\u0c1a\u0c21\u0c3e\u0c28\u0c3f\u0c15\u0c3f \u0c05\u0c28\u0c4d\u200c\u0c32\u0c3e\u0c15\u0c4d \u0c1a\u0c47\u0c2f\u0c02\u0c21\u0c3f."),
    ("unlock_price", "\u0c05\u0c28\u0c4d\u200c\u0c32\u0c3e\u0c15\u0c4d \u0c27\u0c30"),
    ("benefit_phone_number", "\U0001F4DE \u0c2b\u0c4b\u0c28\u0c4d \u0c28\u0c02\u0c2c\u0c30\u0c4d \u0c35\u0c46\u0c02\u0c1f\u0c28\u0c47 \u0c2a\u0c4a\u0c02\u0c26\u0c02\u0c21\u0c3f"),
    ("benefit_direct_communication", "\U0001F4AC \u0c28\u0c47\u0c30\u0c41\u0c17\u0c3e \u0c38\u0c02\u0c2d\u0c3e\u0c37\u0c23"),
    ("benefit_faster_hiring", "\u26A1 \u0c35\u0c47\u0c17\u0c35\u0c02\u0c24\u0c2e\u0c48\u0c28 \u0c28\u0c3f\u0c2f\u0c3e\u0c2e\u0c15 \u0c2a\u0c4d\u0c30\u0c15\u0c4d\u0c30\u0c3f\u0c2f"),
    ("pay_and_unlock", "\u20B9%1$d \u0c1a\u0c46\u0c32\u0c4d\u0c32\u0c3f\u0c02\u0c1a\u0c3f \u0c05\u0c28\u0c4d\u200c\u0c32\u0c3e\u0c15\u0c4d \u0c1a\u0c47\u0c2f\u0c02\u0c21\u0c3f"),
    # EditJobScreen
    ("payment_details", "\u0c1a\u0c46\u0c32\u0c4d\u0c32\u0c3f\u0c02\u0c2a\u0c41 \u0c35\u0c3f\u0c35\u0c30\u0c3e\u0c32\u0c41"),
]


def add_strings(xml_path, strings):
    tree = ET.parse(xml_path)
    root = tree.getroot()
    existing = {elem.get('name') for elem in root.findall('string')}
    added = 0
    for name, value in strings:
        if name not in existing:
            el = ET.SubElement(root, 'string', attrib={'name': name})
            el.text = value
            el.tail = '\n    '
            added += 1
    if added > 0:
        # Fix formatting
        last = list(root)[-1]
        last.tail = '\n'
        tree.write(xml_path, encoding='utf-8', xml_declaration=True)
    return added


en_path = os.path.join(BASE, 'values', 'strings.xml')
te_path = os.path.join(BASE, 'values-te', 'strings.xml')

en_added = add_strings(en_path, EN_STRINGS)
print(f"Added {en_added} English earnings strings")

te_added = add_strings(te_path, TE_STRINGS)
print(f"Added {te_added} Telugu earnings strings")
