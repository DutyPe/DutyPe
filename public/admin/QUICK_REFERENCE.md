# 🚀 Admin Panel - Quick Reference

## 📍 URLs

- **Login:** https://dutypeapp.web.app/admin/login.html
- **Dashboard:** https://dutypeapp.web.app/admin/dashboard.html
- **Post Job:** https://dutypeapp.web.app/admin/post-job.html

## 🔐 Setup (One-Time)

```bash
# 1. Create admin user in Firebase Console
# Go to: Authentication → Users → Add User

# 2. Deploy
firebase deploy --only hosting

# 3. Login
# Visit: https://dutypeapp.web.app/admin/
```

## 📝 Post a Job (Quick)

1. Login → Dashboard
2. Click "Post New Job"
3. Fill required fields:
   - Title, Company, Description
   - Location, Category
   - Pay Amount, Pay Type
   - Job Type, Vacancies
   - Contact Phone
4. Click "Post Job"
5. Done! Job appears in app

## 🎯 Required Fields

| Field | Example |
|-------|---------|
| Job Title | "Delivery Partner" |
| Company | "ABC Restaurant" |
| Description | "Deliver food orders..." |
| Location | "Khammam" |
| Category | "Delivery" |
| Pay Amount | "15000" |
| Pay Type | "per month" |
| Job Type | "Full-time" |
| Vacancies | "5" |
| Contact Phone | "9876543210" |

## 🔧 Common Commands

```bash
# Deploy admin panel
firebase deploy --only hosting

# Create admin user (if using script)
node scripts/create-admin-user.js

# Test locally
firebase serve --only hosting
```

## ❓ Quick Troubleshooting

| Problem | Solution |
|---------|----------|
| Can't login | Check Firebase Console → Authentication → Users |
| Job not showing | Verify `isActive: true` in Firestore |
| Page 404 | Run `firebase deploy --only hosting` |
| Form error | Check all required fields filled |

## 📞 Support

- **Setup Guide:** ADMIN_PANEL_SETUP.md
- **Firebase Console:** https://console.firebase.google.com/
- **Browser Console:** Press F12 for errors

---

**Quick Access:** https://dutypeapp.web.app/admin/
