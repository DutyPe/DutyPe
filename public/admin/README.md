# DutyPe Admin Panel - Setup Guide

## 🚀 Quick Start

Your admin panel is now ready! Follow these steps to set it up:

### Step 1: Create Admin Account in Firebase

1. Go to [Firebase Console](https://console.firebase.google.com/)
2. Select your project: **dutypeapp**
3. Navigate to **Authentication** → **Users**
4. Click **Add User**
5. Enter your admin email and password:
   - Email: `admin@dutype.com` (or your preferred email)
   - Password: Create a strong password (min 6 characters)
6. Click **Add User**

### Step 2: Deploy to Firebase Hosting

Run these commands in your terminal:

```bash
# Deploy to Firebase Hosting
firebase deploy --only hosting
```

### Step 3: Access Your Admin Panel

After deployment, access your admin panel at:

**Login Page:** `https://dutypeapp.web.app/admin/login.html`

**Post Job Page:** `https://dutypeapp.web.app/admin/post-job.html` (redirects to login if not authenticated)

### Step 4: Login and Post Jobs

1. Open the login page
2. Enter your admin credentials
3. You'll be redirected to the job posting form
4. Fill in all required fields and click "Post Job"

## 🔒 Security Features

✅ **Firebase Authentication** - Only authenticated users can access the admin panel
✅ **Auto-redirect** - Unauthenticated users are redirected to login
✅ **Session Management** - Logout functionality included
✅ **Secure** - Uses Firebase Auth for authentication

## 📝 Required Job Fields

When posting a job, you must fill in:

- **Job Title** (e.g., Delivery Partner, Cook)
- **Company Name**
- **Description**
- **Location** (e.g., Khammam, Hyderabad)
- **Category** (Delivery, Driver, Cook, etc.)
- **Pay Amount** (₹)
- **Pay Type** (Per Month, Per Day, Per Hour, Per Delivery)
- **Job Type** (Full-time, Part-time, Contract, Temporary)
- **Vacancies** (Number of openings)
- **Contact Phone**

### Optional Fields:

- Gender Preference
- Shift Timing
- WhatsApp Number
- Latitude/Longitude (for precise location)

## 🎯 Features

- ✨ Clean, modern UI
- 📱 Mobile responsive
- ✅ Form validation
- 🔄 Real-time feedback
- 🚀 Fast job posting
- 🔐 Secure authentication
- 📊 Success/error messages

## 🛠️ Troubleshooting

### Can't login?
- Verify you created the admin user in Firebase Console
- Check email and password are correct
- Ensure Firebase Authentication is enabled

### Job not appearing in app?
- Check Firestore rules allow writes
- Verify the job was created in Firestore Console
- Check `isActive` field is set to `true`

### Page not loading?
- Ensure you deployed to Firebase Hosting
- Check the URL is correct
- Clear browser cache and try again

## 📞 Support

If you need help, check:
1. Firebase Console for errors
2. Browser console for JavaScript errors
3. Firestore rules for permission issues

## 🔐 Admin Credentials

**IMPORTANT:** Keep your admin credentials secure!

- Email: `admin@dutype.com` (or your chosen email)
- Password: (Set in Firebase Console)

**Never share these credentials with anyone!**

---

Made with ❤️ for DutyPe
