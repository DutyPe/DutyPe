import xml.etree.ElementTree as ET

# --- English ---
tree = ET.parse('app/src/main/res/values/strings.xml')
root = tree.getroot()
existing = {el.get('name') for el in root.findall('string')}

new_en = {
    # Refer & Earn - shared
    'refer_complete_profile_title': 'Complete Your Profile',
    'refer_complete_profile_button': 'Complete Profile',
    'refer_code_clipboard_label': 'Referral Code',
    'refer_share_chooser_title': 'Share Referral Code',
    'refer_successful_referrals_count': '%1$d successful referrals',
    'refer_milestone_progress': '%1$d / %2$d referrals',
    'refer_bonus_amount': 'Rs.%1$d bonus',
    'refer_free_posts_count': '%1$d free posts',
    'refer_free_posts_available': '%1$d posts available',
    'refer_expires_date': 'Expires: %1$s',
    'refer_step_2': 'They sign up using your code',
    'refer_step_3': 'You earn Rs.25 and they earn Rs.25 instantly',
    'refer_reward_per_referral': 'You earn Rs.25 for every successful referral',
    'refer_reward_friend_bonus': 'Your friend gets an instant Rs.25 signup bonus',
    'refer_reward_15': '15 referrals: Rs.150 milestone bonus',
    'refer_reward_50': '50 referrals: Rs.500 milestone bonus',
    'refer_reward_100': '100 referrals: Rs.1000 milestone bonus',
    'refer_withdrawal_threshold_info': 'Withdrawals open once your available balance reaches Rs.50.',
    'refer_redeem_step_1': 'Tap the Withdraw button on this screen',
    'refer_redeem_step_2': 'Enter the amount and your UPI ID',
    'refer_redeem_step_3': 'Submit the request for payout review',
    'refer_redeem_step_4': 'Your referral wallet balance updates immediately after the request',
    'refer_min_withdrawal_info': 'Minimum withdrawal: Rs.50',
    'refer_you_earned': 'You: Rs.%1$s',
    'refer_milestone_earned': 'Milestone: Rs.%1$s',
    'refer_status_pending': 'Pending',
    'refer_status_expired': 'Expired',
    'refer_status_cancelled': 'Cancelled',
    'refer_available_balance': 'Available: Rs.%1$d',
    'refer_upi_placeholder': 'yourname@upi',
    'refer_error_min_withdrawal': 'Minimum withdrawal is Rs.%1$d',
    'refer_error_insufficient_balance': 'Insufficient balance',
    'refer_error_enter_upi': 'Enter UPI ID',
    'refer_error_invalid_upi': 'Invalid UPI ID format',
    'refer_performer_from_city': '%1$s from %2$s',
    'refer_more_top_performers': '+%1$d more top performers',
    'refer_legal_not_pyramid': '\\u2022 This is a legitimate referral program, not a pyramid scheme',
    'refer_legal_taxable': '\\u2022 Referral rewards are taxable income under Indian tax laws',
    'refer_legal_kyc': '\\u2022 KYC required for withdrawals > \\u20B910,000/year',
    'refer_legal_pan': '\\u2022 PAN card mandatory for withdrawals > \\u20B950,000/year',
    'refer_legal_fraud': '\\u2022 Fraudulent activity will result in account suspension',
    # Employer-specific
    'refer_employer_complete_profile_desc': 'To generate your personalized referral code and start earning rewards, please complete your company profile first.',
    'refer_employer_share_text': 'Join DutyPe for hiring!\\n\\nUse my referral code: %1$s\\n\\nDownload DutyPe: %2$s\\n\\nFind reliable workers for your business and earn Rs.%3$d bonus!',
    'refer_employer_step_1': 'Share your referral code with employers or workers',
    'refer_employer_step_4': 'Reach milestones for bonus cash and free job posts',
    'refer_reward_5_employer': '5 referrals: Rs.50 bonus + 5 free job posts',
    'refer_reward_10_employer': '10 referrals: Rs.100 bonus + 10 free job posts',
    'refer_reward_25_employer': '25 referrals: Rs.250 bonus + 25 free job posts',
    'refer_signup_bonus_earned': 'Signup bonus: Rs.%1$s',
    # Worker-specific
    'refer_worker_complete_profile_desc': 'To generate your personalized referral code and start earning rewards, please complete your profile first.',
    'refer_worker_share_text': 'Join DutyPe and start earning!\\n\\nUse my referral code: %1$s\\n\\nDownload: %2$s\\n\\nFind local jobs near you and earn Rs.%3$d bonus!',
    'refer_worker_step_1': 'Share your referral code with friends',
    'refer_worker_step_4': 'Hit milestones to unlock extra bonus rewards',
    'refer_reward_5_worker': '5 referrals: Rs.50 milestone bonus',
    'refer_reward_10_worker': '10 referrals: Rs.100 milestone bonus',
    'refer_reward_25_worker': '25 referrals: Rs.250 milestone bonus',
    'refer_friend_bonus_earned': 'Friend bonus: Rs.%1$s',
    'refer_share_friend_bonus': 'Share this code \\u2192 friend gets \\u20B9%1$d bonus!',
    'refer_default_user_name': 'User',
    'code_copied': 'Code copied!',
    'something_went_wrong': 'Something went wrong',
}

added = 0
for name, val in new_en.items():
    if name not in existing:
        el = ET.SubElement(root, 'string')
        el.set('name', name)
        el.text = val
        added += 1

ET.indent(tree, space='    ')
tree.write('app/src/main/res/values/strings.xml', encoding='utf-8', xml_declaration=True)
print(f'Added {added} English refer strings')

# --- Telugu ---
tree_te = ET.parse('app/src/main/res/values-te/strings.xml')
root_te = tree_te.getroot()
existing_te = {el.get('name') for el in root_te.findall('string')}

new_te = {
    'refer_complete_profile_title': '\u0c2e\u0c40 \u0c2a\u0c4d\u0c30\u0c4a\u0c2b\u0c48\u0c32\u0c4d\u200c\u0c28\u0c41 \u0c2a\u0c42\u0c30\u0c4d\u0c24\u0c3f \u0c1a\u0c47\u0c2f\u0c02\u0c21\u0c3f',
    'refer_complete_profile_button': '\u0c2a\u0c4d\u0c30\u0c4a\u0c2b\u0c48\u0c32\u0c4d \u0c2a\u0c42\u0c30\u0c4d\u0c24\u0c3f \u0c1a\u0c47\u0c2f\u0c02\u0c21\u0c3f',
    'refer_code_clipboard_label': '\u0c30\u0c46\u0c2b\u0c30\u0c32\u0c4d \u0c15\u0c4b\u0c21\u0c4d',
    'refer_share_chooser_title': '\u0c30\u0c46\u0c2b\u0c30\u0c32\u0c4d \u0c15\u0c4b\u0c21\u0c4d \u0c37\u0c47\u0c30\u0c4d \u0c1a\u0c47\u0c2f\u0c02\u0c21\u0c3f',
    'refer_successful_referrals_count': '%1$d \u0c35\u0c3f\u0c1c\u0c2f\u0c35\u0c02\u0c24\u0c2e\u0c48\u0c28 \u0c30\u0c46\u0c2b\u0c30\u0c32\u0c4d\u200c\u0c32\u0c41',
    'refer_milestone_progress': '%1$d / %2$d \u0c30\u0c46\u0c2b\u0c30\u0c32\u0c4d\u200c\u0c32\u0c41',
    'refer_bonus_amount': 'Rs.%1$d \u0c2c\u0c4b\u0c28\u0c38\u0c4d',
    'refer_free_posts_count': '%1$d \u0c09\u0c1a\u0c3f\u0c24 \u0c2a\u0c4b\u0c38\u0c4d\u0c1f\u0c4d\u200c\u0c32\u0c41',
    'refer_free_posts_available': '%1$d \u0c2a\u0c4b\u0c38\u0c4d\u0c1f\u0c4d\u200c\u0c32\u0c41 \u0c05\u0c02\u0c26\u0c41\u0c2c\u0c3e\u0c1f\u0c41\u0c32\u0c4b \u0c09\u0c28\u0c4d\u0c28\u0c3e\u0c2f\u0c3f',
    'refer_expires_date': '\u0c17\u0c21\u0c41\u0c35\u0c41: %1$s',
    'refer_step_2': '\u0c35\u0c3e\u0c30\u0c41 \u0c2e\u0c40 \u0c15\u0c4b\u0c21\u0c4d \u0c09\u0c2a\u0c2f\u0c4b\u0c17\u0c3f\u0c02\u0c1a\u0c3f \u0c38\u0c48\u0c28\u0c4d \u0c05\u0c2a\u0c4d \u0c1a\u0c47\u0c38\u0c4d\u0c24\u0c3e\u0c30\u0c41',
    'refer_step_3': '\u0c2e\u0c40\u0c30\u0c41 Rs.25 \u0c2e\u0c30\u0c3f\u0c2f\u0c41 \u0c35\u0c3e\u0c30\u0c41 Rs.25 \u0c35\u0c46\u0c02\u0c1f\u0c28\u0c47 \u0c38\u0c02\u0c2a\u0c3e\u0c26\u0c3f\u0c38\u0c4d\u0c24\u0c3e\u0c30\u0c41',
    'refer_reward_per_referral': '\u0c2a\u0c4d\u0c30\u0c24\u0c3f \u0c35\u0c3f\u0c1c\u0c2f\u0c35\u0c02\u0c24\u0c2e\u0c48\u0c28 \u0c30\u0c46\u0c2b\u0c30\u0c32\u0c4d\u200c\u0c15\u0c41 \u0c2e\u0c40\u0c30\u0c41 Rs.25 \u0c38\u0c02\u0c2a\u0c3e\u0c26\u0c3f\u0c38\u0c4d\u0c24\u0c3e\u0c30\u0c41',
    'refer_reward_friend_bonus': '\u0c2e\u0c40 \u0c38\u0c4d\u0c28\u0c47\u0c39\u0c3f\u0c24\u0c41\u0c21\u0c3f\u0c15\u0c3f Rs.25 \u0c38\u0c48\u0c28\u0c4d \u0c05\u0c2a\u0c4d \u0c2c\u0c4b\u0c28\u0c38\u0c4d \u0c35\u0c46\u0c02\u0c1f\u0c28\u0c47 \u0c35\u0c38\u0c4d\u0c24\u0c41\u0c02\u0c26\u0c3f',
    'refer_reward_15': '15 \u0c30\u0c46\u0c2b\u0c30\u0c32\u0c4d\u200c\u0c32\u0c41: Rs.150 \u0c2e\u0c48\u0c32\u0c41\u0c30\u0c3e\u0c2f\u0c3f \u0c2c\u0c4b\u0c28\u0c38\u0c4d',
    'refer_reward_50': '50 \u0c30\u0c46\u0c2b\u0c30\u0c32\u0c4d\u200c\u0c32\u0c41: Rs.500 \u0c2e\u0c48\u0c32\u0c41\u0c30\u0c3e\u0c2f\u0c3f \u0c2c\u0c4b\u0c28\u0c38\u0c4d',
    'refer_reward_100': '100 \u0c30\u0c46\u0c2b\u0c30\u0c32\u0c4d\u200c\u0c32\u0c41: Rs.1000 \u0c2e\u0c48\u0c32\u0c41\u0c30\u0c3e\u0c2f\u0c3f \u0c2c\u0c4b\u0c28\u0c38\u0c4d',
    'refer_withdrawal_threshold_info': '\u0c2e\u0c40 \u0c05\u0c02\u0c26\u0c41\u0c2c\u0c3e\u0c1f\u0c41\u0c32\u0c4b \u0c09\u0c28\u0c4d\u0c28 \u0c2c\u0c4d\u0c2f\u0c3e\u0c32\u0c46\u0c28\u0c4d\u0c38\u0c4d Rs.50\u0c15\u0c41 \u0c1a\u0c47\u0c30\u0c3f\u0c28 \u0c24\u0c30\u0c4d\u0c35\u0c3e\u0c24 \u0c09\u0c2a\u0c38\u0c02\u0c39\u0c30\u0c23\u0c32\u0c41 \u0c24\u0c46\u0c30\u0c41\u0c1a\u0c41\u0c15\u0c41\u0c02\u0c1f\u0c3e\u0c2f\u0c3f.',
    'refer_redeem_step_1': '\u0c08 \u0c38\u0c4d\u0c15\u0c4d\u0c30\u0c40\u0c28\u0c4d\u200c\u0c32\u0c4b \u0c09\u0c2a\u0c38\u0c02\u0c39\u0c30\u0c3f\u0c02\u0c1a\u0c41 \u0c2c\u0c1f\u0c28\u0c4d \u0c1f\u0c4d\u0c2f\u0c3e\u0c2a\u0c4d \u0c1a\u0c47\u0c2f\u0c02\u0c21\u0c3f',
    'refer_redeem_step_2': '\u0c2e\u0c4a\u0c24\u0c4d\u0c24\u0c02 \u0c2e\u0c30\u0c3f\u0c2f\u0c41 \u0c2e\u0c40 UPI ID \u0c28\u0c2e\u0c4b\u0c26\u0c41 \u0c1a\u0c47\u0c2f\u0c02\u0c21\u0c3f',
    'refer_redeem_step_3': '\u0c1a\u0c46\u0c32\u0c4d\u0c32\u0c3f\u0c02\u0c2a\u0c41 \u0c38\u0c2e\u0c40\u0c15\u0c4d\u0c37 \u0c15\u0c4b\u0c38\u0c02 \u0c05\u0c2d\u0c4d\u0c2f\u0c30\u0c4d\u0c25\u0c28\u0c28\u0c41 \u0c38\u0c2e\u0c30\u0c4d\u0c2a\u0c3f\u0c02\u0c1a\u0c02\u0c21\u0c3f',
    'refer_redeem_step_4': '\u0c05\u0c2d\u0c4d\u0c2f\u0c30\u0c4d\u0c25\u0c28 \u0c24\u0c30\u0c4d\u0c35\u0c3e\u0c24 \u0c2e\u0c40 \u0c30\u0c46\u0c2b\u0c30\u0c32\u0c4d \u0c35\u0c3e\u0c32\u0c46\u0c1f\u0c4d \u0c2c\u0c4d\u0c2f\u0c3e\u0c32\u0c46\u0c28\u0c4d\u0c38\u0c4d \u0c35\u0c46\u0c02\u0c1f\u0c28\u0c47 \u0c05\u0c2a\u0c4d\u200c\u0c21\u0c47\u0c1f\u0c4d \u0c05\u0c35\u0c41\u0c24\u0c41\u0c02\u0c26\u0c3f',
    'refer_min_withdrawal_info': '\u0c15\u0c28\u0c40\u0c38 \u0c09\u0c2a\u0c38\u0c02\u0c39\u0c30\u0c23: Rs.50',
    'refer_you_earned': '\u0c2e\u0c40\u0c30\u0c41: Rs.%1$s',
    'refer_milestone_earned': '\u0c2e\u0c48\u0c32\u0c41\u0c30\u0c3e\u0c2f\u0c3f: Rs.%1$s',
    'refer_status_pending': '\u0c2a\u0c46\u0c02\u0c21\u0c3f\u0c02\u0c17\u0c4d',
    'refer_status_expired': '\u0c17\u0c21\u0c41\u0c35\u0c41 \u0c2e\u0c41\u0c17\u0c3f\u0c38\u0c3f\u0c02\u0c26\u0c3f',
    'refer_status_cancelled': '\u0c30\u0c26\u0c4d\u0c26\u0c41 \u0c1a\u0c47\u0c2f\u0c2c\u0c21\u0c3f\u0c02\u0c26\u0c3f',
    'refer_available_balance': '\u0c05\u0c02\u0c26\u0c41\u0c2c\u0c3e\u0c1f\u0c41\u0c32\u0c4b: Rs.%1$d',
    'refer_upi_placeholder': 'yourname@upi',
    'refer_error_min_withdrawal': '\u0c15\u0c28\u0c40\u0c38 \u0c09\u0c2a\u0c38\u0c02\u0c39\u0c30\u0c23 Rs.%1$d',
    'refer_error_insufficient_balance': '\u0c38\u0c30\u0c3f\u0c2a\u0c21\u0c28\u0c3f \u0c2c\u0c4d\u0c2f\u0c3e\u0c32\u0c46\u0c28\u0c4d\u0c38\u0c4d',
    'refer_error_enter_upi': 'UPI ID \u0c28\u0c2e\u0c4b\u0c26\u0c41 \u0c1a\u0c47\u0c2f\u0c02\u0c21\u0c3f',
    'refer_error_invalid_upi': '\u0c1a\u0c46\u0c32\u0c4d\u0c32\u0c28\u0c3f UPI ID \u0c2b\u0c3e\u0c30\u0c4d\u0c2e\u0c3e\u0c1f\u0c4d',
    'refer_performer_from_city': '%1$s \u0c28\u0c41\u0c02\u0c21\u0c3f %2$s',
    'refer_more_top_performers': '+%1$d \u0c2e\u0c30\u0c3f\u0c28\u0c4d\u0c28\u0c3f \u0c1f\u0c3e\u0c2a\u0c4d \u0c2a\u0c30\u0c4d\u0c2b\u0c3e\u0c30\u0c4d\u0c2e\u0c30\u0c4d\u200c\u0c32\u0c41',
    'refer_legal_not_pyramid': '\u2022 \u0c07\u0c26\u0c3f \u0c1a\u0c1f\u0c4d\u0c1f\u0c2c\u0c26\u0c4d\u0c27\u0c2e\u0c48\u0c28 \u0c30\u0c46\u0c2b\u0c30\u0c32\u0c4d \u0c2a\u0c4d\u0c30\u0c4b\u0c17\u0c4d\u0c30\u0c3e\u0c2e\u0c4d, \u0c2a\u0c3f\u0c30\u0c2e\u0c3f\u0c21\u0c4d \u0c38\u0c4d\u0c15\u0c40\u0c2e\u0c4d \u0c15\u0c3e\u0c26\u0c41',
    'refer_legal_taxable': '\u2022 \u0c30\u0c46\u0c2b\u0c30\u0c32\u0c4d \u0c30\u0c3f\u0c35\u0c3e\u0c30\u0c4d\u0c21\u0c4d\u200c\u0c32\u0c41 \u0c2d\u0c3e\u0c30\u0c24 \u0c2a\u0c28\u0c4d\u0c28\u0c41 \u0c1a\u0c1f\u0c4d\u0c1f\u0c3e\u0c32 \u0c2a\u0c4d\u0c30\u0c15\u0c3e\u0c30\u0c02 \u0c2a\u0c28\u0c4d\u0c28\u0c41 \u0c35\u0c3f\u0c27\u0c3f\u0c02\u0c1a\u0c26\u0c17\u0c3f\u0c28 \u0c06\u0c26\u0c3e\u0c2f\u0c02',
    'refer_legal_kyc': '\u2022 \u20B910,000/\u0c38\u0c02\u0c35\u0c24\u0c4d\u0c38\u0c30\u0c02 \u0c15\u0c02\u0c1f\u0c47 \u0c0e\u0c15\u0c4d\u0c15\u0c41\u0c35 \u0c09\u0c2a\u0c38\u0c02\u0c39\u0c30\u0c23\u0c32\u0c15\u0c41 KYC \u0c05\u0c35\u0c38\u0c30\u0c02',
    'refer_legal_pan': '\u2022 \u20B950,000/\u0c38\u0c02\u0c35\u0c24\u0c4d\u0c38\u0c30\u0c02 \u0c15\u0c02\u0c1f\u0c47 \u0c0e\u0c15\u0c4d\u0c15\u0c41\u0c35 \u0c09\u0c2a\u0c38\u0c02\u0c39\u0c30\u0c23\u0c32\u0c15\u0c41 PAN \u0c15\u0c3e\u0c30\u0c4d\u0c21\u0c4d \u0c24\u0c2a\u0c4d\u0c2a\u0c28\u0c3f\u0c38\u0c30\u0c3f',
    'refer_legal_fraud': '\u2022 \u0c2e\u0c4b\u0c38\u0c2a\u0c42\u0c30\u0c3f\u0c24 \u0c15\u0c3e\u0c30\u0c4d\u0c2f\u0c15\u0c32\u0c3e\u0c2a\u0c02 \u0c16\u0c3e\u0c24\u0c3e \u0c28\u0c3f\u0c32\u0c41\u0c2a\u0c41\u0c26\u0c32\u0c15\u0c41 \u0c26\u0c3e\u0c30\u0c3f \u0c24\u0c40\u0c38\u0c4d\u0c24\u0c41\u0c02\u0c26\u0c3f',
    # Employer-specific
    'refer_employer_complete_profile_desc': '\u0c2e\u0c40 \u0c35\u0c4d\u0c2f\u0c15\u0c4d\u0c24\u0c3f\u0c17\u0c24 \u0c30\u0c46\u0c2b\u0c30\u0c32\u0c4d \u0c15\u0c4b\u0c21\u0c4d\u200c\u0c28\u0c41 \u0c1c\u0c46\u0c28\u0c30\u0c47\u0c1f\u0c4d \u0c1a\u0c47\u0c2f\u0c21\u0c3e\u0c28\u0c3f\u0c15\u0c3f \u0c2e\u0c30\u0c3f\u0c2f\u0c41 \u0c30\u0c3f\u0c35\u0c3e\u0c30\u0c4d\u0c21\u0c4d\u200c\u0c32\u0c41 \u0c38\u0c02\u0c2a\u0c3e\u0c26\u0c3f\u0c02\u0c1a\u0c21\u0c02 \u0c2a\u0c4d\u0c30\u0c3e\u0c30\u0c02\u0c2d\u0c3f\u0c02\u0c1a\u0c21\u0c3e\u0c28\u0c3f\u0c15\u0c3f, \u0c26\u0c2f\u0c1a\u0c47\u0c38\u0c3f \u0c2e\u0c41\u0c02\u0c26\u0c41\u0c17\u0c3e \u0c2e\u0c40 \u0c15\u0c02\u0c2a\u0c46\u0c28\u0c40 \u0c2a\u0c4d\u0c30\u0c4a\u0c2b\u0c48\u0c32\u0c4d\u200c\u0c28\u0c41 \u0c2a\u0c42\u0c30\u0c4d\u0c24\u0c3f \u0c1a\u0c47\u0c2f\u0c02\u0c21\u0c3f.',
    'refer_employer_share_text': '\u0c28\u0c47\u0c2e\u0c15\u0c3e\u0c32 \u0c15\u0c4b\u0c38\u0c02 DutyPe\u0c32\u0c4b \u0c1a\u0c47\u0c30\u0c02\u0c21\u0c3f!\\n\\n\u0c28\u0c3e \u0c30\u0c46\u0c2b\u0c30\u0c32\u0c4d \u0c15\u0c4b\u0c21\u0c4d \u0c09\u0c2a\u0c2f\u0c4b\u0c17\u0c3f\u0c02\u0c1a\u0c02\u0c21\u0c3f: %1$s\\n\\nDutyPe \u0c21\u0c4c\u0c28\u0c4d\u200c\u0c32\u0c4b\u0c21\u0c4d: %2$s\\n\\n\u0c2e\u0c40 \u0c35\u0c4d\u0c2f\u0c3e\u0c2a\u0c3e\u0c30\u0c02 \u0c15\u0c4b\u0c38\u0c02 \u0c28\u0c2e\u0c4d\u0c2e\u0c15\u0c2e\u0c48\u0c28 \u0c15\u0c3e\u0c30\u0c4d\u0c2e\u0c3f\u0c15\u0c41\u0c32\u0c28\u0c41 \u0c15\u0c28\u0c41\u0c17\u0c4a\u0c28\u0c02\u0c21\u0c3f \u0c2e\u0c30\u0c3f\u0c2f\u0c41 Rs.%3$d \u0c2c\u0c4b\u0c28\u0c38\u0c4d \u0c38\u0c02\u0c2a\u0c3e\u0c26\u0c3f\u0c02\u0c1a\u0c02\u0c21\u0c3f!',
    'refer_employer_step_1': '\u0c2e\u0c40 \u0c30\u0c46\u0c2b\u0c30\u0c32\u0c4d \u0c15\u0c4b\u0c21\u0c4d\u200c\u0c28\u0c41 \u0c2f\u0c1c\u0c2e\u0c3e\u0c28\u0c41\u0c32\u0c41 \u0c32\u0c47\u0c26\u0c3e \u0c15\u0c3e\u0c30\u0c4d\u0c2e\u0c3f\u0c15\u0c41\u0c32\u0c24\u0c4b \u0c37\u0c47\u0c30\u0c4d \u0c1a\u0c47\u0c2f\u0c02\u0c21\u0c3f',
    'refer_employer_step_4': '\u0c2c\u0c4b\u0c28\u0c38\u0c4d \u0c28\u0c17\u0c26\u0c41 \u0c2e\u0c30\u0c3f\u0c2f\u0c41 \u0c09\u0c1a\u0c3f\u0c24 \u0c09\u0c26\u0c4d\u0c2f\u0c4b\u0c17 \u0c2a\u0c4b\u0c38\u0c4d\u0c1f\u0c4d\u200c\u0c32 \u0c15\u0c4b\u0c38\u0c02 \u0c2e\u0c48\u0c32\u0c41\u0c30\u0c3e\u0c2f\u0c3f\u0c32\u0c28\u0c41 \u0c1a\u0c47\u0c30\u0c41\u0c15\u0c4b\u0c02\u0c21\u0c3f',
    'refer_reward_5_employer': '5 \u0c30\u0c46\u0c2b\u0c30\u0c32\u0c4d\u200c\u0c32\u0c41: Rs.50 \u0c2c\u0c4b\u0c28\u0c38\u0c4d + 5 \u0c09\u0c1a\u0c3f\u0c24 \u0c09\u0c26\u0c4d\u0c2f\u0c4b\u0c17 \u0c2a\u0c4b\u0c38\u0c4d\u0c1f\u0c4d\u200c\u0c32\u0c41',
    'refer_reward_10_employer': '10 \u0c30\u0c46\u0c2b\u0c30\u0c32\u0c4d\u200c\u0c32\u0c41: Rs.100 \u0c2c\u0c4b\u0c28\u0c38\u0c4d + 10 \u0c09\u0c1a\u0c3f\u0c24 \u0c09\u0c26\u0c4d\u0c2f\u0c4b\u0c17 \u0c2a\u0c4b\u0c38\u0c4d\u0c1f\u0c4d\u200c\u0c32\u0c41',
    'refer_reward_25_employer': '25 \u0c30\u0c46\u0c2b\u0c30\u0c32\u0c4d\u200c\u0c32\u0c41: Rs.250 \u0c2c\u0c4b\u0c28\u0c38\u0c4d + 25 \u0c09\u0c1a\u0c3f\u0c24 \u0c09\u0c26\u0c4d\u0c2f\u0c4b\u0c17 \u0c2a\u0c4b\u0c38\u0c4d\u0c1f\u0c4d\u200c\u0c32\u0c41',
    'refer_signup_bonus_earned': '\u0c38\u0c48\u0c28\u0c4d \u0c05\u0c2a\u0c4d \u0c2c\u0c4b\u0c28\u0c38\u0c4d: Rs.%1$s',
    # Worker-specific
    'refer_worker_complete_profile_desc': '\u0c2e\u0c40 \u0c35\u0c4d\u0c2f\u0c15\u0c4d\u0c24\u0c3f\u0c17\u0c24 \u0c30\u0c46\u0c2b\u0c30\u0c32\u0c4d \u0c15\u0c4b\u0c21\u0c4d\u200c\u0c28\u0c41 \u0c1c\u0c46\u0c28\u0c30\u0c47\u0c1f\u0c4d \u0c1a\u0c47\u0c2f\u0c21\u0c3e\u0c28\u0c3f\u0c15\u0c3f \u0c2e\u0c30\u0c3f\u0c2f\u0c41 \u0c30\u0c3f\u0c35\u0c3e\u0c30\u0c4d\u0c21\u0c4d\u200c\u0c32\u0c41 \u0c38\u0c02\u0c2a\u0c3e\u0c26\u0c3f\u0c02\u0c1a\u0c21\u0c02 \u0c2a\u0c4d\u0c30\u0c3e\u0c30\u0c02\u0c2d\u0c3f\u0c02\u0c1a\u0c21\u0c3e\u0c28\u0c3f\u0c15\u0c3f, \u0c26\u0c2f\u0c1a\u0c47\u0c38\u0c3f \u0c2e\u0c41\u0c02\u0c26\u0c41\u0c17\u0c3e \u0c2e\u0c40 \u0c2a\u0c4d\u0c30\u0c4a\u0c2b\u0c48\u0c32\u0c4d\u200c\u0c28\u0c41 \u0c2a\u0c42\u0c30\u0c4d\u0c24\u0c3f \u0c1a\u0c47\u0c2f\u0c02\u0c21\u0c3f.',
    'refer_worker_share_text': 'DutyPe\u0c32\u0c4b \u0c1a\u0c47\u0c30\u0c3f \u0c38\u0c02\u0c2a\u0c3e\u0c26\u0c3f\u0c02\u0c1a\u0c21\u0c02 \u0c2a\u0c4d\u0c30\u0c3e\u0c30\u0c02\u0c2d\u0c3f\u0c02\u0c1a\u0c02\u0c21\u0c3f!\\n\\n\u0c28\u0c3e \u0c30\u0c46\u0c2b\u0c30\u0c32\u0c4d \u0c15\u0c4b\u0c21\u0c4d \u0c09\u0c2a\u0c2f\u0c4b\u0c17\u0c3f\u0c02\u0c1a\u0c02\u0c21\u0c3f: %1$s\\n\\n\u0c21\u0c4c\u0c28\u0c4d\u200c\u0c32\u0c4b\u0c21\u0c4d: %2$s\\n\\n\u0c2e\u0c40 \u0c38\u0c2e\u0c40\u0c2a\u0c02\u0c32\u0c4b \u0c38\u0c4d\u0c25\u0c3e\u0c28\u0c3f\u0c15 \u0c09\u0c26\u0c4d\u0c2f\u0c4b\u0c17\u0c3e\u0c32\u0c28\u0c41 \u0c15\u0c28\u0c41\u0c17\u0c4a\u0c28\u0c02\u0c21\u0c3f \u0c2e\u0c30\u0c3f\u0c2f\u0c41 Rs.%3$d \u0c2c\u0c4b\u0c28\u0c38\u0c4d \u0c38\u0c02\u0c2a\u0c3e\u0c26\u0c3f\u0c02\u0c1a\u0c02\u0c21\u0c3f!',
    'refer_worker_step_1': '\u0c2e\u0c40 \u0c30\u0c46\u0c2b\u0c30\u0c32\u0c4d \u0c15\u0c4b\u0c21\u0c4d\u200c\u0c28\u0c41 \u0c38\u0c4d\u0c28\u0c47\u0c39\u0c3f\u0c24\u0c41\u0c32\u0c24\u0c4b \u0c37\u0c47\u0c30\u0c4d \u0c1a\u0c47\u0c2f\u0c02\u0c21\u0c3f',
    'refer_worker_step_4': '\u0c05\u0c26\u0c28\u0c2a\u0c41 \u0c2c\u0c4b\u0c28\u0c38\u0c4d \u0c30\u0c3f\u0c35\u0c3e\u0c30\u0c4d\u0c21\u0c4d\u200c\u0c32\u0c28\u0c41 \u0c05\u0c28\u0c4d\u200c\u0c32\u0c3e\u0c15\u0c4d \u0c1a\u0c47\u0c2f\u0c21\u0c3e\u0c28\u0c3f\u0c15\u0c3f \u0c2e\u0c48\u0c32\u0c41\u0c30\u0c3e\u0c2f\u0c3f\u0c32\u0c28\u0c41 \u0c1a\u0c47\u0c30\u0c41\u0c15\u0c4b\u0c02\u0c21\u0c3f',
    'refer_reward_5_worker': '5 \u0c30\u0c46\u0c2b\u0c30\u0c32\u0c4d\u200c\u0c32\u0c41: Rs.50 \u0c2e\u0c48\u0c32\u0c41\u0c30\u0c3e\u0c2f\u0c3f \u0c2c\u0c4b\u0c28\u0c38\u0c4d',
    'refer_reward_10_worker': '10 \u0c30\u0c46\u0c2b\u0c30\u0c32\u0c4d\u200c\u0c32\u0c41: Rs.100 \u0c2e\u0c48\u0c32\u0c41\u0c30\u0c3e\u0c2f\u0c3f \u0c2c\u0c4b\u0c28\u0c38\u0c4d',
    'refer_reward_25_worker': '25 \u0c30\u0c46\u0c2b\u0c30\u0c32\u0c4d\u200c\u0c32\u0c41: Rs.250 \u0c2e\u0c48\u0c32\u0c41\u0c30\u0c3e\u0c2f\u0c3f \u0c2c\u0c4b\u0c28\u0c38\u0c4d',
    'refer_friend_bonus_earned': '\u0c38\u0c4d\u0c28\u0c47\u0c39\u0c3f\u0c24\u0c41\u0c21\u0c3f \u0c2c\u0c4b\u0c28\u0c38\u0c4d: Rs.%1$s',
    'refer_share_friend_bonus': '\u0c08 \u0c15\u0c4b\u0c21\u0c4d \u0c37\u0c47\u0c30\u0c4d \u0c1a\u0c47\u0c2f\u0c02\u0c21\u0c3f \u2192 \u0c38\u0c4d\u0c28\u0c47\u0c39\u0c3f\u0c24\u0c41\u0c21\u0c3f\u0c15\u0c3f \u20B9%1$d \u0c2c\u0c4b\u0c28\u0c38\u0c4d!',
    'refer_default_user_name': '\u0c35\u0c3e\u0c21\u0c15\u0c02',
    'code_copied': '\u0c15\u0c4b\u0c21\u0c4d \u0c15\u0c3e\u0c2a\u0c40 \u0c05\u0c2f\u0c3f\u0c02\u0c26\u0c3f!',
    'something_went_wrong': '\u0c0f\u0c26\u0c4b \u0c24\u0c2a\u0c4d\u0c2a\u0c41 \u0c1c\u0c30\u0c3f\u0c17\u0c3f\u0c02\u0c26\u0c3f',
}

added_te = 0
for name, val in new_te.items():
    if name not in existing_te:
        el = ET.SubElement(root_te, 'string')
        el.set('name', name)
        el.text = val
        added_te += 1

ET.indent(tree_te, space='    ')
tree_te.write('app/src/main/res/values-te/strings.xml', encoding='utf-8', xml_declaration=True)
print(f'Added {added_te} Telugu refer strings')
