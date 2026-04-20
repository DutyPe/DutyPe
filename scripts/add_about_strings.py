import xml.etree.ElementTree as ET

# --- English strings ---
tree = ET.parse('app/src/main/res/values/strings.xml')
root = tree.getroot()
existing = {el.get('name') for el in root.findall('string')}

new_en = {
    'about_worker_subtitle': 'Your gateway to reliable local jobs \u2014 daily, hourly, full-time.',
    'about_worker_mission': 'To empower workers by providing easy access to local job opportunities. Everyone deserves a chance to earn, grow, and succeed without complicated applications or lengthy processes.',
    'about_worker_vision': "To become India\u2019s most trusted platform for local employment, where every worker can find meaningful work that fits their skills, schedule, and location.",
    'about_feat_quick_apply': 'Quick one-tap job applications',
    'about_feat_jobs_near': 'Jobs near your location',
    'about_feat_realtime_notif': 'Real-time job notifications',
    'about_feat_save_jobs': 'Save jobs for later',
    'about_feat_track_apps': 'Track your applications',
    'about_feat_build_profile': 'Build your work profile',
    'about_cat_delivery': 'Delivery &amp; Logistics',
    'about_cat_food': 'Food Service &amp; Cooking',
    'about_cat_housekeeping': 'Housekeeping &amp; Cleaning',
    'about_cat_shop': 'Shop &amp; Retail Help',
    'about_cat_childcare': 'Childcare &amp; Eldercare',
    'about_cat_maintenance': 'Maintenance &amp; Repairs',
    'about_why_no_resume': 'No resume required',
    'about_why_verified_employers': 'Verified employers',
    'about_why_transparent_pay': 'Transparent pay information',
    'about_why_flexible_work': 'Flexible work options',
    'about_why_safe_platform': 'Safe and secure platform',
    'about_employer_mission': 'To empower businesses by providing a seamless, efficient, and reliable platform to connect with a flexible workforce \u2014 so you can focus on growing your business.',
    'about_employer_vision': "To become the leading platform for on-demand employment in India, where businesses thrive with the right talent and workers find meaningful opportunities.",
    'about_emp_feat_post_jobs': 'Post jobs in minutes',
    'about_emp_feat_talent_pool': 'Access a large talent pool',
    'about_emp_feat_gps_attendance': 'GPS-based attendance tracking',
    'about_emp_feat_verified_workers': 'Verified worker profiles',
    'about_emp_feat_flexible_hiring': 'Flexible hiring options',
    'about_emp_feat_realtime_alerts': 'Real-time application alerts',
    'about_emp_feat_manage_postings': 'Manage multiple postings',
    'about_emp_feat_track_performance': 'Track worker performance',
    'about_emp_cat_delivery': 'Delivery Personnel',
    'about_emp_cat_kitchen': 'Kitchen &amp; Cooking Staff',
    'about_emp_cat_housekeeping': 'Housekeeping &amp; Cleaning',
    'about_emp_cat_shop': 'Shop Assistants &amp; Retail',
    'about_emp_cat_childcare': 'Childcare &amp; Eldercare',
    'about_emp_cat_maintenance_w': 'Maintenance Workers',
    'about_emp_cat_event': 'Event &amp; Catering Staff',
    'about_emp_why_quick_hiring': 'Quick hiring process',
    'about_emp_why_verified_db': 'Verified worker database',
    'about_emp_why_cost_effective': 'Cost-effective solutions',
    'about_emp_why_24_7': '24/7 platform access',
    'about_emp_why_support': 'Dedicated support team',
    'about_emp_val_efficiency': 'Efficiency in hiring',
    'about_emp_val_reliability': 'Reliability you can trust',
    'about_emp_val_transparency': 'Transparency in all dealings',
    'about_emp_val_empowerment': 'Empowerment for businesses',
    'about_made_in_bharat': 'Made with \u2764\ufe0f in Bharat',
    'about_version_format': 'Version %1$s',
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
print(f'Added {added} English About strings')

# --- Telugu strings ---
tree_te = ET.parse('app/src/main/res/values-te/strings.xml')
root_te = tree_te.getroot()
existing_te = {el.get('name') for el in root_te.findall('string')}

new_te = {
    'about_worker_subtitle': '\u0c28\u0c2e\u0c4d\u0c2e\u0c15\u0c2e\u0c48\u0c28 \u0c38\u0c4d\u0c25\u0c3e\u0c28\u0c3f\u0c15 \u0c09\u0c26\u0c4d\u0c2f\u0c4b\u0c17\u0c3e\u0c32\u0c15\u0c41 \u0c2e\u0c40 \u0c17\u0c47\u0c1f\u0c4d\u200c\u0c35\u0c47 \u2014 \u0c30\u0c4b\u0c1c\u0c41\u0c35\u0c3e\u0c30\u0c40, \u0c17\u0c02\u0c1f\u0c32 \u0c35\u0c3e\u0c30\u0c40, \u0c2a\u0c42\u0c30\u0c4d\u0c24\u0c3f-\u0c38\u0c2e\u0c2f\u0c02.',
    'about_worker_mission': '\u0c15\u0c37\u0c4d\u0c1f\u0c2e\u0c48\u0c28 \u0c26\u0c30\u0c16\u0c3e\u0c38\u0c4d\u0c24\u0c41\u0c32\u0c41 \u0c32\u0c47\u0c26\u0c3e \u0c38\u0c41\u0c26\u0c40\u0c30\u0c4d\u0c18 \u0c2a\u0c4d\u0c30\u0c15\u0c4d\u0c30\u0c3f\u0c2f\u0c32\u0c41 \u0c32\u0c47\u0c15\u0c41\u0c02\u0c21\u0c3e \u0c38\u0c4d\u0c25\u0c3e\u0c28\u0c3f\u0c15 \u0c09\u0c26\u0c4d\u0c2f\u0c4b\u0c17 \u0c05\u0c35\u0c15\u0c3e\u0c36\u0c3e\u0c32\u0c15\u0c41 \u0c38\u0c41\u0c32\u0c2d\u0c2e\u0c48\u0c28 \u0c2a\u0c4d\u0c30\u0c3e\u0c2a\u0c4d\u0c2f\u0c24\u0c28\u0c41 \u0c05\u0c02\u0c26\u0c3f\u0c02\u0c1a\u0c21\u0c02 \u0c26\u0c4d\u0c35\u0c3e\u0c30\u0c3e \u0c15\u0c3e\u0c30\u0c4d\u0c2e\u0c3f\u0c15\u0c41\u0c32\u0c28\u0c41 \u0c36\u0c15\u0c4d\u0c24\u0c3f\u0c2e\u0c02\u0c24\u0c02 \u0c1a\u0c47\u0c2f\u0c21\u0c02. \u0c2a\u0c4d\u0c30\u0c24\u0c3f \u0c12\u0c15\u0c4d\u0c15\u0c30\u0c42 \u0c38\u0c02\u0c2a\u0c3e\u0c26\u0c3f\u0c02\u0c1a\u0c21\u0c3e\u0c28\u0c3f\u0c15\u0c3f, \u0c2a\u0c46\u0c30\u0c17\u0c21\u0c3e\u0c28\u0c3f\u0c15\u0c3f \u0c2e\u0c30\u0c3f\u0c2f\u0c41 \u0c35\u0c3f\u0c1c\u0c2f\u0c02 \u0c38\u0c3e\u0c27\u0c3f\u0c02\u0c1a\u0c21\u0c3e\u0c28\u0c3f\u0c15\u0c3f \u0c05\u0c30\u0c4d\u0c39\u0c41\u0c32\u0c41.',
    'about_worker_vision': '\u0c2a\u0c4d\u0c30\u0c24\u0c3f \u0c15\u0c3e\u0c30\u0c4d\u0c2e\u0c3f\u0c15\u0c41\u0c21\u0c41 \u0c24\u0c2e \u0c28\u0c48\u0c2a\u0c41\u0c23\u0c4d\u0c2f\u0c3e\u0c32\u0c15\u0c41, \u0c37\u0c46\u0c21\u0c4d\u0c2f\u0c42\u0c32\u0c4d\u200c\u0c15\u0c41 \u0c2e\u0c30\u0c3f\u0c2f\u0c41 \u0c2a\u0c4d\u0c30\u0c3e\u0c02\u0c24\u0c3e\u0c28\u0c3f\u0c15\u0c3f \u0c38\u0c30\u0c3f\u0c2a\u0c4b\u0c2f\u0c47 \u0c05\u0c30\u0c4d\u0c25\u0c35\u0c02\u0c24\u0c2e\u0c48\u0c28 \u0c2a\u0c28\u0c3f\u0c28\u0c3f \u0c15\u0c28\u0c41\u0c17\u0c4a\u0c28\u0c47 \u0c38\u0c4d\u0c25\u0c3e\u0c28\u0c3f\u0c15 \u0c09\u0c2a\u0c3e\u0c27\u0c3f\u0c15\u0c3f \u0c2d\u0c3e\u0c30\u0c24\u0c26\u0c47\u0c36\u0c02\u0c32\u0c4b \u0c05\u0c24\u0c4d\u0c2f\u0c02\u0c24 \u0c28\u0c2e\u0c4d\u0c2e\u0c15\u0c2e\u0c48\u0c28 \u0c35\u0c47\u0c26\u0c3f\u0c15\u0c17\u0c3e \u0c2e\u0c3e\u0c30\u0c21\u0c02.',
    'about_feat_quick_apply': '\u0c12\u0c15\u0c4d\u0c15 \u0c1f\u0c4d\u0c2f\u0c3e\u0c2a\u0c4d\u200c\u0c24\u0c4b \u0c24\u0c4d\u0c35\u0c30\u0c3f\u0c24 \u0c09\u0c26\u0c4d\u0c2f\u0c4b\u0c17 \u0c26\u0c30\u0c16\u0c3e\u0c38\u0c4d\u0c24\u0c41\u0c32\u0c41',
    'about_feat_jobs_near': '\u0c2e\u0c40 \u0c2a\u0c4d\u0c30\u0c3e\u0c02\u0c24\u0c02\u0c32\u0c4b \u0c09\u0c26\u0c4d\u0c2f\u0c4b\u0c17\u0c3e\u0c32\u0c41',
    'about_feat_realtime_notif': '\u0c30\u0c3f\u0c2f\u0c32\u0c4d-\u0c1f\u0c48\u0c2e\u0c4d \u0c09\u0c26\u0c4d\u0c2f\u0c4b\u0c17 \u0c28\u0c4b\u0c1f\u0c3f\u0c2b\u0c3f\u0c15\u0c47\u0c37\u0c28\u0c4d\u0c32\u0c41',
    'about_feat_save_jobs': '\u0c09\u0c26\u0c4d\u0c2f\u0c4b\u0c17\u0c3e\u0c32\u0c28\u0c41 \u0c24\u0c30\u0c4d\u0c35\u0c3e\u0c24 \u0c15\u0c4b\u0c38\u0c02 \u0c38\u0c47\u0c35\u0c4d \u0c1a\u0c47\u0c2f\u0c02\u0c21\u0c3f',
    'about_feat_track_apps': '\u0c2e\u0c40 \u0c26\u0c30\u0c16\u0c3e\u0c38\u0c4d\u0c24\u0c41\u0c32\u0c28\u0c41 \u0c1f\u0c4d\u0c30\u0c3e\u0c15\u0c4d \u0c1a\u0c47\u0c2f\u0c02\u0c21\u0c3f',
    'about_feat_build_profile': '\u0c2e\u0c40 \u0c2a\u0c28\u0c3f \u0c2a\u0c4d\u0c30\u0c4a\u0c2b\u0c48\u0c32\u0c4d\u200c\u0c28\u0c41 \u0c30\u0c42\u0c2a\u0c4a\u0c02\u0c26\u0c3f\u0c02\u0c1a\u0c41\u0c15\u0c4b\u0c02\u0c21\u0c3f',
    'about_cat_delivery': '\u0c21\u0c46\u0c32\u0c3f\u0c35\u0c30\u0c40 & \u0c32\u0c3e\u0c1c\u0c3f\u0c38\u0c4d\u0c1f\u0c3f\u0c15\u0c4d\u0c38\u0c4d',
    'about_cat_food': '\u0c06\u0c39\u0c3e\u0c30 \u0c38\u0c47\u0c35 & \u0c35\u0c02\u0c1f',
    'about_cat_housekeeping': '\u0c17\u0c43\u0c39 \u0c28\u0c3f\u0c30\u0c4d\u0c35\u0c39\u0c23 & \u0c36\u0c41\u0c2d\u0c4d\u0c30\u0c02',
    'about_cat_shop': '\u0c37\u0c3e\u0c2a\u0c41 & \u0c30\u0c3f\u0c1f\u0c48\u0c32\u0c4d \u0c38\u0c39\u0c3e\u0c2f\u0c02',
    'about_cat_childcare': '\u0c2a\u0c3f\u0c32\u0c4d\u0c32\u0c32 & \u0c35\u0c43\u0c26\u0c4d\u0c27\u0c41\u0c32 \u0c38\u0c02\u0c30\u0c15\u0c4d\u0c37\u0c23',
    'about_cat_maintenance': '\u0c28\u0c3f\u0c30\u0c4d\u0c35\u0c39\u0c23 & \u0c2e\u0c30\u0c2e\u0c4d\u0c2e\u0c24\u0c41\u0c32\u0c41',
    'about_why_no_resume': '\u0c30\u0c46\u0c1c\u0c4d\u0c2f\u0c42\u0c2e\u0c4d \u0c05\u0c35\u0c38\u0c30\u0c02 \u0c32\u0c47\u0c26\u0c41',
    'about_why_verified_employers': '\u0c27\u0c43\u0c35\u0c40\u0c15\u0c30\u0c3f\u0c02\u0c1a\u0c3f\u0c28 \u0c2f\u0c1c\u0c2e\u0c3e\u0c28\u0c41\u0c32\u0c41',
    'about_why_transparent_pay': '\u0c2a\u0c3e\u0c30\u0c26\u0c30\u0c4d\u0c36\u0c15\u0c2e\u0c48\u0c28 \u0c1a\u0c46\u0c32\u0c4d\u0c32\u0c3f\u0c02\u0c2a\u0c41 \u0c38\u0c2e\u0c3e\u0c1a\u0c3e\u0c30\u0c02',
    'about_why_flexible_work': '\u0c35\u0c3f\u0c28\u0c2e\u0c4d\u0c30\u0c2e\u0c48\u0c28 \u0c2a\u0c28\u0c3f \u0c0e\u0c02\u0c2a\u0c3f\u0c15\u0c32\u0c41',
    'about_why_safe_platform': '\u0c38\u0c41\u0c30\u0c15\u0c4d\u0c37\u0c3f\u0c24\u0c2e\u0c48\u0c28 \u0c35\u0c47\u0c26\u0c3f\u0c15',
    'about_employer_mission': '\u0c35\u0c4d\u0c2f\u0c3e\u0c2a\u0c3e\u0c30\u0c3e\u0c32\u0c28\u0c41 \u0c35\u0c43\u0c26\u0c4d\u0c27\u0c3f \u0c1a\u0c47\u0c38\u0c41\u0c15\u0c4b\u0c35\u0c21\u0c3e\u0c28\u0c3f\u0c15\u0c3f \u0c35\u0c3f\u0c28\u0c2e\u0c4d\u0c30\u0c2e\u0c48\u0c28 \u0c36\u0c4d\u0c30\u0c2e\u0c36\u0c15\u0c4d\u0c24\u0c3f\u0c24\u0c4b \u0c05\u0c28\u0c41\u0c38\u0c02\u0c27\u0c3e\u0c28\u0c02 \u0c1a\u0c47\u0c2f\u0c21\u0c3e\u0c28\u0c3f\u0c15\u0c3f \u0c38\u0c41\u0c32\u0c2d\u0c2e\u0c48\u0c28, \u0c38\u0c2e\u0c30\u0c4d\u0c27\u0c35\u0c02\u0c24\u0c2e\u0c48\u0c28 \u0c2e\u0c30\u0c3f\u0c2f\u0c41 \u0c28\u0c2e\u0c4d\u0c2e\u0c15\u0c2e\u0c48\u0c28 \u0c35\u0c47\u0c26\u0c3f\u0c15\u0c28\u0c41 \u0c05\u0c02\u0c26\u0c3f\u0c02\u0c1a\u0c21\u0c02 \u0c26\u0c4d\u0c35\u0c3e\u0c30\u0c3e \u0c35\u0c4d\u0c2f\u0c3e\u0c2a\u0c3e\u0c30\u0c3e\u0c32\u0c28\u0c41 \u0c36\u0c15\u0c4d\u0c24\u0c3f\u0c2e\u0c02\u0c24\u0c02 \u0c1a\u0c47\u0c2f\u0c21\u0c02.',
    'about_employer_vision': '\u0c38\u0c30\u0c48\u0c28 \u0c2a\u0c4d\u0c30\u0c24\u0c3f\u0c2d \u0c2e\u0c30\u0c3f\u0c2f\u0c41 \u0c15\u0c3e\u0c30\u0c4d\u0c2e\u0c3f\u0c15\u0c41\u0c32\u0c15\u0c41 \u0c05\u0c30\u0c4d\u0c25\u0c35\u0c02\u0c24\u0c2e\u0c48\u0c28 \u0c05\u0c35\u0c15\u0c3e\u0c36\u0c3e\u0c32\u0c41 \u0c26\u0c4a\u0c30\u0c3f\u0c15\u0c47 \u0c2d\u0c3e\u0c30\u0c24\u0c26\u0c47\u0c36\u0c02\u0c32\u0c4b \u0c05\u0c02\u0c21\u0c2e\u0c3e\u0c02\u0c21\u0c4d \u0c09\u0c2a\u0c3e\u0c27\u0c3f\u0c15\u0c3f \u0c05\u0c17\u0c4d\u0c30\u0c17\u0c23\u0c4d\u0c2f \u0c35\u0c47\u0c26\u0c3f\u0c15\u0c17\u0c3e \u0c2e\u0c3e\u0c30\u0c21\u0c02.',
    'about_emp_feat_post_jobs': '\u0c28\u0c3f\u0c2e\u0c3f\u0c37\u0c3e\u0c32\u0c32\u0c4b \u0c09\u0c26\u0c4d\u0c2f\u0c4b\u0c17\u0c3e\u0c32\u0c28\u0c41 \u0c2a\u0c4b\u0c38\u0c4d\u0c1f\u0c4d \u0c1a\u0c47\u0c2f\u0c02\u0c21\u0c3f',
    'about_emp_feat_talent_pool': '\u0c2a\u0c46\u0c26\u0c4d\u0c26 \u0c2a\u0c4d\u0c30\u0c24\u0c3f\u0c2d \u0c2a\u0c42\u0c32\u0c4d\u200c\u0c15\u0c41 \u0c2a\u0c4d\u0c30\u0c3e\u0c2a\u0c4d\u0c2f\u0c24',
    'about_emp_feat_gps_attendance': 'GPS-\u0c06\u0c27\u0c3e\u0c30\u0c3f\u0c24 \u0c39\u0c3e\u0c1c\u0c30\u0c40 \u0c1f\u0c4d\u0c30\u0c3e\u0c15\u0c3f\u0c02\u0c17\u0c4d',
    'about_emp_feat_verified_workers': '\u0c27\u0c43\u0c35\u0c40\u0c15\u0c30\u0c3f\u0c02\u0c1a\u0c3f\u0c28 \u0c15\u0c3e\u0c30\u0c4d\u0c2e\u0c3f\u0c15 \u0c2a\u0c4d\u0c30\u0c4a\u0c2b\u0c48\u0c32\u0c4d\u200c\u0c32\u0c41',
    'about_emp_feat_flexible_hiring': '\u0c35\u0c3f\u0c28\u0c2e\u0c4d\u0c30\u0c2e\u0c48\u0c28 \u0c28\u0c47\u0c2e\u0c15\u0c02 \u0c0e\u0c02\u0c2a\u0c3f\u0c15\u0c32\u0c41',
    'about_emp_feat_realtime_alerts': '\u0c30\u0c3f\u0c2f\u0c32\u0c4d-\u0c1f\u0c48\u0c2e\u0c4d \u0c26\u0c30\u0c16\u0c3e\u0c38\u0c4d\u0c24\u0c41 \u0c05\u0c32\u0c30\u0c4d\u0c1f\u0c4d\u200c\u0c32\u0c41',
    'about_emp_feat_manage_postings': '\u0c05\u0c28\u0c47\u0c15 \u0c2a\u0c4b\u0c38\u0c4d\u0c1f\u0c3f\u0c02\u0c17\u0c4d\u200c\u0c32\u0c28\u0c41 \u0c28\u0c3f\u0c30\u0c4d\u0c35\u0c39\u0c3f\u0c02\u0c1a\u0c02\u0c21\u0c3f',
    'about_emp_feat_track_performance': '\u0c15\u0c3e\u0c30\u0c4d\u0c2e\u0c3f\u0c15 \u0c2a\u0c28\u0c3f\u0c24\u0c40\u0c30\u0c41\u0c28\u0c41 \u0c1f\u0c4d\u0c30\u0c3e\u0c15\u0c4d \u0c1a\u0c47\u0c2f\u0c02\u0c21\u0c3f',
    'about_emp_cat_delivery': '\u0c21\u0c46\u0c32\u0c3f\u0c35\u0c30\u0c40 \u0c38\u0c3f\u0c2c\u0c4d\u0c2c\u0c02\u0c26\u0c3f',
    'about_emp_cat_kitchen': '\u0c35\u0c02\u0c1f\u0c17\u0c26\u0c3f & \u0c35\u0c02\u0c1f \u0c38\u0c3f\u0c2c\u0c4d\u0c2c\u0c02\u0c26\u0c3f',
    'about_emp_cat_housekeeping': '\u0c17\u0c43\u0c39 \u0c28\u0c3f\u0c30\u0c4d\u0c35\u0c39\u0c23 & \u0c36\u0c41\u0c2d\u0c4d\u0c30\u0c02',
    'about_emp_cat_shop': '\u0c37\u0c3e\u0c2a\u0c41 \u0c38\u0c39\u0c3e\u0c2f\u0c15\u0c41\u0c32\u0c41 & \u0c30\u0c3f\u0c1f\u0c48\u0c32\u0c4d',
    'about_emp_cat_childcare': '\u0c2a\u0c3f\u0c32\u0c4d\u0c32\u0c32 & \u0c35\u0c43\u0c26\u0c4d\u0c27\u0c41\u0c32 \u0c38\u0c02\u0c30\u0c15\u0c4d\u0c37\u0c23',
    'about_emp_cat_maintenance_w': '\u0c28\u0c3f\u0c30\u0c4d\u0c35\u0c39\u0c23 \u0c15\u0c3e\u0c30\u0c4d\u0c2e\u0c3f\u0c15\u0c41\u0c32\u0c41',
    'about_emp_cat_event': '\u0c08\u0c35\u0c46\u0c02\u0c1f\u0c4d & \u0c15\u0c47\u0c1f\u0c30\u0c3f\u0c02\u0c17\u0c4d \u0c38\u0c3f\u0c2c\u0c4d\u0c2c\u0c02\u0c26\u0c3f',
    'about_emp_why_quick_hiring': '\u0c24\u0c4d\u0c35\u0c30\u0c3f\u0c24 \u0c28\u0c47\u0c2e\u0c15\u0c02 \u0c2a\u0c4d\u0c30\u0c15\u0c4d\u0c30\u0c3f\u0c2f',
    'about_emp_why_verified_db': '\u0c27\u0c43\u0c35\u0c40\u0c15\u0c30\u0c3f\u0c02\u0c1a\u0c3f\u0c28 \u0c15\u0c3e\u0c30\u0c4d\u0c2e\u0c3f\u0c15 \u0c21\u0c47\u0c1f\u0c3e\u0c2c\u0c47\u0c38\u0c4d',
    'about_emp_why_cost_effective': '\u0c16\u0c30\u0c4d\u0c1a\u0c41-\u0c38\u0c2e\u0c30\u0c4d\u0c25\u0c2e\u0c48\u0c28 \u0c2a\u0c30\u0c3f\u0c37\u0c4d\u0c15\u0c3e\u0c30\u0c3e\u0c32\u0c41',
    'about_emp_why_24_7': '24/7 \u0c35\u0c47\u0c26\u0c3f\u0c15 \u0c2a\u0c4d\u0c30\u0c3e\u0c2a\u0c4d\u0c2f\u0c24',
    'about_emp_why_support': '\u0c05\u0c02\u0c15\u0c3f\u0c24 \u0c38\u0c2a\u0c4b\u0c30\u0c4d\u0c1f\u0c4d \u0c1c\u0c1f\u0c4d\u0c1f\u0c41',
    'about_emp_val_efficiency': '\u0c28\u0c47\u0c2e\u0c15\u0c02\u0c32\u0c4b \u0c38\u0c3e\u0c2e\u0c30\u0c4d\u0c25\u0c4d\u0c2f\u0c02',
    'about_emp_val_reliability': '\u0c28\u0c2e\u0c4d\u0c2e\u0c26\u0c17\u0c3f\u0c28 \u0c28\u0c2e\u0c4d\u0c2e\u0c15\u0c24\u0c4d\u0c35\u0c02',
    'about_emp_val_transparency': '\u0c05\u0c28\u0c4d\u0c28\u0c3f \u0c35\u0c4d\u0c2f\u0c35\u0c39\u0c3e\u0c30\u0c3e\u0c32\u0c32\u0c4b \u0c2a\u0c3e\u0c30\u0c26\u0c30\u0c4d\u0c36\u0c15\u0c24',
    'about_emp_val_empowerment': '\u0c35\u0c4d\u0c2f\u0c3e\u0c2a\u0c3e\u0c30\u0c3e\u0c32\u0c15\u0c41 \u0c36\u0c15\u0c4d\u0c24\u0c3f\u0c28\u0c3f\u0c35\u0c4d\u0c35\u0c21\u0c02',
    'about_made_in_bharat': '\u0c2d\u0c3e\u0c30\u0c24\u0c4d\u200c\u0c32\u0c4b \u2764\ufe0f\u0c24\u0c4b \u0c24\u0c2f\u0c3e\u0c30\u0c41 \u0c1a\u0c47\u0c36\u0c3e\u0c02',
    'about_version_format': '\u0c35\u0c46\u0c30\u0c4d\u0c37\u0c28\u0c4d %1$s',
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
print(f'Added {added_te} Telugu About strings')
