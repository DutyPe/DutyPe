/**
 * Non-IT Job Templates across DutyPe categories for Indian Cities
 */

const TEMPLATES_BY_CATEGORY = {
  COOK: [
    {
      titlePrefix: "Cook Required for Veg & Non-Veg Restaurant",
      employmentType: "FULL_TIME",
      shift: "DAY",
      gender: "ANY",
      exp: "1-2 Years",
      edu: "No Formal Education Required",
      minPay: 18000,
      maxPay: 28000,
      payType: "MONTHLY",
      vacancies: 2,
      desc: "Experienced Cook required to prepare authentic South Indian meals, tiffins, and curries. Must maintain kitchen hygiene and food quality standards. Daily meals and tea provided.",
      benefits: ["Food Provided", "Accommodation", "Weekly Off", "Festival Bonus"]
    },
    {
      titlePrefix: "Home Cook (Morning & Evening)",
      employmentType: "PART_TIME",
      shift: "DAY",
      gender: "ANY",
      exp: "1+ Years",
      edu: "None",
      minPay: 9000,
      maxPay: 15000,
      payType: "MONTHLY",
      vacancies: 1,
      desc: "Looking for an experienced home cook for a family of 4. Responsibilities include preparing breakfast, lunch, and dinner. Clean cooking habits required.",
      benefits: ["Flexible Timings", "Sunday Half Day", "Bonus on Festivals"]
    },
    {
      titlePrefix: "Fast Food & Chinese Cook",
      employmentType: "FULL_TIME",
      shift: "ROTATIONAL",
      gender: "MALE",
      exp: "1-3 Years",
      edu: "10th Pass",
      minPay: 16000,
      maxPay: 24000,
      payType: "MONTHLY",
      vacancies: 2,
      desc: "Hiring fast food chef for fried rice, noodles, rolls, and snacks. Fast preparation and neat workstation maintenance needed. Immediate joining.",
      benefits: ["Food Provided", "Overtime Pay", "Weekly Off"]
    }
  ],
  MAID: [
    {
      titlePrefix: "Housemaid & Cleaning Helper",
      employmentType: "FULL_TIME",
      shift: "DAY",
      gender: "FEMALE",
      exp: "Fresher / Any",
      edu: "None",
      minPay: 10000,
      maxPay: 16000,
      payType: "MONTHLY",
      vacancies: 2,
      desc: "Housemaid needed for dusting, sweeping, mopping, and vessel washing. Timings 8 AM to 5 PM. Friendly work environment and safe household.",
      benefits: ["Food Provided", "Tea & Snacks", "Paid Leave", "Festival Bonus"]
    },
    {
      titlePrefix: "Part-Time Domestic Maid",
      employmentType: "PART_TIME",
      shift: "DAY",
      gender: "FEMALE",
      exp: "Fresher / Any",
      edu: "None",
      minPay: 6000,
      maxPay: 9000,
      payType: "MONTHLY",
      vacancies: 1,
      desc: "Morning shift domestic help for 3 hours (7:30 AM to 10:30 AM). Sweeping, mopping floors, and utensil cleaning. Punctual and reliable person needed.",
      benefits: ["Flexible Timing", "Paid Weekly Off"]
    }
  ],
  DRIVER: [
    {
      titlePrefix: "Personal Car Driver",
      employmentType: "FULL_TIME",
      shift: "DAY",
      gender: "MALE",
      exp: "2+ Years",
      edu: "10th Pass",
      minPay: 18000,
      maxPay: 25000,
      payType: "MONTHLY",
      vacancies: 1,
      desc: "Experienced personal driver required for private family car (automatic & manual). Valid commercial/LMV driving license and clean driving record mandatory. Local city routes knowledge essential.",
      benefits: ["Overtime Pay", "Uniform Allowance", "Yearly Bonus", "Sunday Off"]
    },
    {
      titlePrefix: "Commercial Goods Auto / Tata Ace Driver",
      employmentType: "FULL_TIME",
      shift: "DAY",
      gender: "MALE",
      exp: "1-2 Years",
      edu: "8th Pass",
      minPay: 16000,
      maxPay: 22000,
      payType: "MONTHLY",
      vacancies: 3,
      desc: "Driver required for local city delivery of goods in Tata Ace / pickup vehicle. Must have valid driving license with badge. Fuel and maintenance handled by company.",
      benefits: ["Incentives per Trip", "ESI & PF", "Weekly Fuel Allowance"]
    },
    {
      titlePrefix: "School Bus / Van Driver",
      employmentType: "FULL_TIME",
      shift: "DAY",
      gender: "MALE",
      exp: "3+ Years",
      edu: "10th Pass",
      minPay: 17000,
      maxPay: 23000,
      payType: "MONTHLY",
      vacancies: 2,
      desc: "Responsible driver required for student pickup and drop routes. Heavy vehicle license and patient driving demeanor strictly required.",
      benefits: ["Free Lunch", "PF & ESI", "School Holidays Paid"]
    }
  ],
  DELIVERY: [
    {
      titlePrefix: "Delivery Partner (Bike Mandatory)",
      employmentType: "FULL_TIME",
      shift: "ROTATIONAL",
      gender: "ANY",
      exp: "Fresher / Any",
      edu: "10th Pass",
      minPay: 18000,
      maxPay: 30000,
      payType: "MONTHLY",
      vacancies: 10,
      desc: "Hiring delivery partners for food and grocery orders. Earn weekly payouts plus daily petrol incentives. Android phone, two-wheeler, and driving license required.",
      benefits: ["Per Order Incentive", "Petrol Allowance", "Insurance Coverage", "Flexible Shifts"]
    },
    {
      titlePrefix: "Courier & Parcel Delivery Executive",
      employmentType: "FULL_TIME",
      shift: "DAY",
      gender: "MALE",
      exp: "Fresher / Any",
      edu: "10th Pass",
      minPay: 15000,
      maxPay: 22000,
      payType: "MONTHLY",
      vacancies: 5,
      desc: "Deliver packages within assigned pin-code radius. Bag and scanner app provided. Fixed salary plus per-packet delivery bonus.",
      benefits: ["Petrol Reimbursement", "ESI & PF", "Weekly Off", "Mobile Allowance"]
    }
  ],
  SECURITY: [
    {
      titlePrefix: "Security Guard (Day / Night Shift)",
      employmentType: "FULL_TIME",
      shift: "ROTATIONAL",
      gender: "MALE",
      exp: "Fresher / Any",
      edu: "10th Pass",
      minPay: 14000,
      maxPay: 19000,
      payType: "MONTHLY",
      vacancies: 4,
      desc: "Security guard needed for commercial complex and gated community. Maintaining visitor registers, gate vigilance, and vehicle parking management. 8 or 12 hour shifts available.",
      benefits: ["Free Uniform", "ESI & PF", "Accommodation Provided", "Overtime Extra"]
    },
    {
      titlePrefix: "Security Supervisor",
      employmentType: "FULL_TIME",
      shift: "DAY",
      gender: "MALE",
      exp: "2+ Years",
      edu: "12th Pass",
      minPay: 18000,
      maxPay: 26000,
      payType: "MONTHLY",
      vacancies: 2,
      desc: "Supervising gate guards, patrolling facility premises, managing shift rosters, and handling CCTV surveillance checks. Ex-servicemen or experienced candidates preferred.",
      benefits: ["ESI & PF", "Travel Allowance", "Weekly Off"]
    }
  ],
  ELECTRICIAN: [
    {
      titlePrefix: "Licensed Electrician / Wireman",
      employmentType: "FULL_TIME",
      shift: "DAY",
      gender: "MALE",
      exp: "1-3 Years",
      edu: "ITI Electrical",
      minPay: 17000,
      maxPay: 26000,
      payType: "MONTHLY",
      vacancies: 3,
      desc: "Hiring skilled electrician for residential and commercial electrical wiring, MCB installations, lighting fixtures, and fan repairs. Two-wheeler preferred.",
      benefits: ["Tool Kit Allowance", "Fuel Allowance", "Incentives on Service Calls", "PF"]
    },
    {
      titlePrefix: "Appliance & Electrical Maintenance Tech",
      employmentType: "FULL_TIME",
      shift: "DAY",
      gender: "MALE",
      exp: "1+ Years",
      edu: "ITI / Diploma",
      minPay: 16000,
      maxPay: 24000,
      payType: "MONTHLY",
      vacancies: 2,
      desc: "Field technician for motor repairs, switchboard maintenance, and home appliance electrical troubleshooting. Immediate joining.",
      benefits: ["Performance Incentives", "Weekly Off", "Travel Allowance"]
    }
  ],
  PLUMBER: [
    {
      titlePrefix: "Plumber & Sanitary Technician",
      employmentType: "FULL_TIME",
      shift: "DAY",
      gender: "MALE",
      exp: "1-3 Years",
      edu: "ITI / Practical Experience",
      minPay: 16000,
      maxPay: 25000,
      payType: "MONTHLY",
      vacancies: 3,
      desc: "Plumber needed for pipeline laying, tap fixing, bathroom fitting installations, and water tank pipeline repairs. Candidate must know CPVC and PVC piping.",
      benefits: ["Daily Travel Allowance", "Per-Job Incentive", "Tool Kit Provided"]
    }
  ],
  CARPENTER: [
    {
      titlePrefix: "Furniture & Interior Carpenter",
      employmentType: "FULL_TIME",
      shift: "DAY",
      gender: "MALE",
      exp: "2+ Years",
      edu: "Practical Experience",
      minPay: 18000,
      maxPay: 28000,
      payType: "MONTHLY",
      vacancies: 2,
      desc: "Woodwork carpenter for modular kitchen fitting, wardrobe assembly, door lock fixing, and plywood cutting. Site-based work with reliable daily transport assistance.",
      benefits: ["Daily Lunch Allowance", "Overtime Pay", "Festival Bonus"]
    }
  ],
  PAINTER: [
    {
      titlePrefix: "Building & Wall Painter",
      employmentType: "FULL_TIME",
      shift: "DAY",
      gender: "MALE",
      exp: "1+ Years",
      edu: "None",
      minPay: 15000,
      maxPay: 22000,
      payType: "MONTHLY",
      vacancies: 4,
      desc: "Interior and exterior wall painting, putty application, primer coating, and enamel polishing. Daily wage or monthly fixed pay available based on experience.",
      benefits: ["Daily Tea & Snacks", "Overtime Allowance", "Weekly Cash Advances"]
    }
  ],
  MECHANIC: [
    {
      titlePrefix: "Two-Wheeler / Bike Mechanic",
      employmentType: "FULL_TIME",
      shift: "DAY",
      gender: "MALE",
      exp: "1-3 Years",
      edu: "ITI / Practical",
      minPay: 16000,
      maxPay: 24000,
      payType: "MONTHLY",
      vacancies: 2,
      desc: "Mechanic for multi-brand two-wheeler general servicing, engine tuning, brake repairs, and wiring checks at busy authorized service point.",
      benefits: ["Performance Incentives", "Tool Kit Provided", "Sunday Off"]
    },
    {
      titlePrefix: "Car Mechanic & AC Technician",
      employmentType: "FULL_TIME",
      shift: "DAY",
      gender: "MALE",
      exp: "2+ Years",
      edu: "ITI Automobile",
      minPay: 20000,
      maxPay: 30000,
      payType: "MONTHLY",
      vacancies: 2,
      desc: "Automobile technician for periodic car maintenance, suspension, brake pads, coolant replacement, and AC gas refill. Workshop environment with modern diagnostic tools.",
      benefits: ["Incentives", "PF & ESI", "Uniform Provided"]
    }
  ],
  SALES: [
    {
      titlePrefix: "Retail Sales Executive (Clothing & Textiles)",
      employmentType: "FULL_TIME",
      shift: "DAY",
      gender: "ANY",
      exp: "Fresher / Any",
      edu: "10th / 12th Pass",
      minPay: 13000,
      maxPay: 20000,
      payType: "MONTHLY",
      vacancies: 5,
      desc: "Greeting customers, showing garments and dress materials, maintaining showroom display racks, and assisting with customer selections. Pleasant communication skills required.",
      benefits: ["Monthly Sales Incentive", "Store Discount", "ESI & PF", "Weekly Off"]
    },
    {
      titlePrefix: "Counter Sales Associate (Electronics & Mobiles)",
      employmentType: "FULL_TIME",
      shift: "DAY",
      gender: "ANY",
      exp: "6 Months - 1 Year",
      edu: "12th Pass / Any Degree",
      minPay: 15000,
      maxPay: 23000,
      payType: "MONTHLY",
      vacancies: 3,
      desc: "Explaining smartphone features, home appliances, and accessories to retail walk-in customers. Achieving monthly counter targets. Attractive commissions on accessories.",
      benefits: ["High Sales Incentives", "ESI & PF", "Mobile Allowance"]
    },
    {
      titlePrefix: "Jewellery Showroom Sales Staff",
      employmentType: "FULL_TIME",
      shift: "DAY",
      gender: "FEMALE",
      exp: "1+ Years",
      edu: "12th Pass",
      minPay: 16000,
      maxPay: 24000,
      payType: "MONTHLY",
      vacancies: 4,
      desc: "Counter sales representative for gold, silver, and diamond ornaments. Welcoming clients, handling product showcases with utmost care, and guiding purchases.",
      benefits: ["Uniform Provided", "Store Incentive", "PF", "Weekly Off"]
    }
  ],
  CASHIER: [
    {
      titlePrefix: "Billing Cashier (Supermarket / Mart)",
      employmentType: "FULL_TIME",
      shift: "DAY",
      gender: "ANY",
      exp: "Fresher / Any",
      edu: "12th Pass / Graduate",
      minPay: 14000,
      maxPay: 20000,
      payType: "MONTHLY",
      vacancies: 3,
      desc: "Barcode scanning, billing via POS software, collecting cash and UPI payments, issuing receipt slips, and tallying daily closing cash balance.",
      benefits: ["ESI & PF", "Overtime Pay", "Uniform Provided", "Weekly Off"]
    }
  ],
  PACKER: [
    {
      titlePrefix: "Warehouse Packing & Sorting Helper",
      employmentType: "FULL_TIME",
      shift: "DAY",
      gender: "ANY",
      exp: "Fresher / Any",
      edu: "10th Pass",
      minPay: 13000,
      maxPay: 18000,
      payType: "MONTHLY",
      vacancies: 8,
      desc: "Packing online grocery and retail products into boxes, labeling with barcodes, checking expiry dates, and loading onto dispatch carts.",
      benefits: ["Attendance Bonus", "ESI & PF", "Overtime Extra", "Subsidized Food"]
    }
  ],
  HELPER: [
    {
      titlePrefix: "General Store & Loading Helper",
      employmentType: "FULL_TIME",
      shift: "DAY",
      gender: "MALE",
      exp: "Fresher / Any",
      edu: "None",
      minPay: 12000,
      maxPay: 17000,
      payType: "MONTHLY",
      vacancies: 4,
      desc: "Assisting in wholesale grocery store with unloading trucks, arranging sacks in godown, and delivering orders to local customer vehicles.",
      benefits: ["Tea & Snacks", "Daily Cash Tips", "Yearly Bonus"]
    },
    {
      titlePrefix: "Hospital / Clinic Ward Helper",
      employmentType: "FULL_TIME",
      shift: "ROTATIONAL",
      gender: "ANY",
      exp: "Fresher / Any",
      edu: "10th Pass",
      minPay: 13000,
      maxPay: 18000,
      payType: "MONTHLY",
      vacancies: 3,
      desc: "Assisting ward staff, transporting wheelchairs/stretchers, changing bed sheets, and maintaining neat ward premises. Compassionate attitude required.",
      benefits: ["ESI & PF", "Uniform Provided", "Night Shift Allowance"]
    }
  ],
  WAITER: [
    {
      titlePrefix: "Restaurant Waiter & Steward",
      employmentType: "FULL_TIME",
      shift: "ROTATIONAL",
      gender: "MALE",
      exp: "Fresher / Any",
      edu: "10th Pass",
      minPay: 13000,
      maxPay: 19000,
      payType: "MONTHLY",
      vacancies: 5,
      desc: "Welcoming diners, taking food orders, serving dishes promptly, clearing dining tables, and maintaining table readiness. Polite communication required.",
      benefits: ["Free Food & Accommodation", "Daily Tips", "Weekly Off"]
    }
  ],
  RECEPTIONIST: [
    {
      titlePrefix: "Front Desk Receptionist (Hospital / Clinic)",
      employmentType: "FULL_TIME",
      shift: "DAY",
      gender: "FEMALE",
      exp: "6 Months - 1 Year",
      edu: "12th Pass / Graduate",
      minPay: 15000,
      maxPay: 22000,
      payType: "MONTHLY",
      vacancies: 2,
      desc: "Managing patient reception, scheduling doctor appointments, issuing consultation tokens, entering patient details in system, and answering incoming telephone calls.",
      benefits: ["ESI & PF", "Friendly Healthcare Environment", "Weekly Off"]
    },
    {
      titlePrefix: "Hotel Front Office Receptionist",
      employmentType: "FULL_TIME",
      shift: "ROTATIONAL",
      gender: "ANY",
      exp: "1+ Years",
      edu: "Any Degree",
      minPay: 16000,
      maxPay: 24000,
      payType: "MONTHLY",
      vacancies: 2,
      desc: "Checking in guests, issuing room keycards, processing card/UPI payments, coordinating with housekeeping, and assisting guests with luggage and transport.",
      benefits: ["Duty Meal Provided", "ESI & PF", "Overtime Pay"]
    }
  ],
  TELECALLER: [
    {
      titlePrefix: "Telecaller (Telugu / English / Hindi)",
      employmentType: "FULL_TIME",
      shift: "DAY",
      gender: "ANY",
      exp: "Fresher / Any",
      edu: "12th Pass / Any Degree",
      minPay: 14000,
      maxPay: 22000,
      payType: "MONTHLY",
      vacancies: 6,
      desc: "Making outbound calls to prospective customers for service inquiries, banking products, and loan eligibility checks. No field work; comfortable AC office setup.",
      benefits: ["Per-Sale Incentives", "ESI & PF", "Weekly Off", "Performance Bonus"]
    }
  ],
  OFFICE_STAFF: [
    {
      titlePrefix: "Office Boy / Admin Assistant",
      employmentType: "FULL_TIME",
      shift: "DAY",
      gender: "MALE",
      exp: "Fresher / Any",
      edu: "10th Pass",
      minPay: 12000,
      maxPay: 16000,
      payType: "MONTHLY",
      vacancies: 2,
      desc: "Serving tea/water to guests, filing office documents, photocopying/scanning, operating office equipment, and running nearby bank/postal errands.",
      benefits: ["ESI & PF", "Fixed Day Shift", "Sunday Off", "Yearly Bonus"]
    }
  ],
  FIELD_EXECUTIVE: [
    {
      titlePrefix: "Field Collection & Verification Executive",
      employmentType: "FULL_TIME",
      shift: "DAY",
      gender: "MALE",
      exp: "Fresher / Any",
      edu: "10th / 12th Pass",
      minPay: 16000,
      maxPay: 24000,
      payType: "MONTHLY",
      vacancies: 4,
      desc: "Field visits for customer address verification and retail collection. Mobile app tracking and receipt generation. Two-wheeler and driving license required.",
      benefits: ["Petrol Allowance", "Mobile Reimbursement", "Incentives on Recovery", "ESI"]
    }
  ],
  MARKETING: [
    {
      titlePrefix: "Field Marketing & Promotion Executive",
      employmentType: "FULL_TIME",
      shift: "DAY",
      gender: "ANY",
      exp: "Fresher / Any",
      edu: "12th Pass / Degree",
      minPay: 16000,
      maxPay: 25000,
      payType: "MONTHLY",
      vacancies: 4,
      desc: "Promoting local businesses, installing banners, visiting retail shops to introduce new products, and distributing marketing pamphlets.",
      benefits: ["Daily Travel Allowance", "Performance Incentives", "Weekly Off"]
    }
  ],
  BEAUTICIAN: [
    {
      titlePrefix: "Beautician & Hair Stylist",
      employmentType: "FULL_TIME",
      shift: "DAY",
      gender: "FEMALE",
      exp: "1-3 Years",
      edu: "Beautician Course Certificate",
      minPay: 16000,
      maxPay: 26000,
      payType: "MONTHLY",
      vacancies: 2,
      desc: "Hair styling, threading, waxing, facial treatments, and bridal makeup at established women's beauty salon. Friendly and supportive team environment.",
      benefits: ["Commission on Services", "Product Discount", "Tips", "Weekly Off"]
    }
  ],
  TAILOR: [
    {
      titlePrefix: "Boutique Tailor & Master Cutter",
      employmentType: "FULL_TIME",
      shift: "DAY",
      gender: "ANY",
      exp: "2+ Years",
      edu: "Tailoring Experience",
      minPay: 18000,
      maxPay: 28000,
      payType: "MONTHLY",
      vacancies: 2,
      desc: "Blouse stitching, kurti cutting, churidar alterations, and designer dress finishing. Precision stitching with modern motorized sewing machines.",
      benefits: ["Per Piece Bonus", "Overtime Extra", "Tea & Snacks"]
    }
  ],
  PHARMACY: [
    {
      titlePrefix: "Pharmacy Assistant / Medical Store Helper",
      employmentType: "FULL_TIME",
      shift: "ROTATIONAL",
      gender: "ANY",
      exp: "6 Months - 1 Year",
      edu: "D.Pharm / 12th Science",
      minPay: 15000,
      maxPay: 22000,
      payType: "MONTHLY",
      vacancies: 3,
      desc: "Dispensing medicines as per doctor prescription, checking expiry dates on shelves, entering medicine stock in billing computer, and attending walk-in patients.",
      benefits: ["Medicine Discount", "ESI & PF", "Overtime Pay", "Weekly Off"]
    }
  ],
  HEALTHCARE: [
    {
      titlePrefix: "Home Care Nurse / Patient Attendant",
      employmentType: "FULL_TIME",
      shift: "DAY",
      gender: "ANY",
      exp: "1+ Years",
      edu: "ANM / GNM / Nursing Assistant",
      minPay: 18000,
      maxPay: 28000,
      payType: "MONTHLY",
      vacancies: 3,
      desc: "Assisting elderly or recovering patients at home with timely medication, BP/sugar checks, mobility support, and personal hygiene. Respectful and quiet family setting.",
      benefits: ["Meals Provided", "Night Shift Allowance", "Paid Leave"]
    }
  ],
  TEACHER: [
    {
      titlePrefix: "Primary School Teacher (All Subjects)",
      employmentType: "FULL_TIME",
      shift: "DAY",
      gender: "FEMALE",
      exp: "1-2 Years",
      edu: "B.Ed / D.Ed / Any Graduate",
      minPay: 15000,
      maxPay: 24000,
      payType: "MONTHLY",
      vacancies: 3,
      desc: "Teaching classes 1 to 5, conducting interactive classroom activities, checking homework notebooks, and participating in school events.",
      benefits: ["PF Included", "Summer Vacation Paid", "School Bus Free Travel"]
    }
  ],
  TUTOR: [
    {
      titlePrefix: "Home Tutor (Maths & Science, Classes 6-10)",
      employmentType: "PART_TIME",
      shift: "DAY",
      gender: "ANY",
      exp: "1+ Years",
      edu: "B.Sc / B.Tech / Any Graduate",
      minPay: 8000,
      maxPay: 15000,
      payType: "MONTHLY",
      vacancies: 2,
      desc: "Taking evening tuitions (5 PM to 7:30 PM) for school students in Mathematics and Science. Concept clearing and regular test papers.",
      benefits: ["Flexible Evening Hours", "Close to Home"]
    }
  ],
  CONSTRUCTION: [
    {
      titlePrefix: "Construction Site Mason & Helper",
      employmentType: "FULL_TIME",
      shift: "DAY",
      gender: "MALE",
      exp: "1+ Years",
      edu: "None",
      minPay: 15000,
      maxPay: 22000,
      payType: "MONTHLY",
      vacancies: 6,
      desc: "Masonry, bricklaying, concrete mixing, and material carrying at residential building project. Timely weekly or monthly settlements.",
      benefits: ["Accommodation at Site", "Daily Water & Tea", "Overtime Paid"]
    }
  ],
  DATA_ENTRY: [
    {
      titlePrefix: "Data Entry Operator & Computer Assistant",
      employmentType: "FULL_TIME",
      shift: "DAY",
      gender: "ANY",
      exp: "Fresher / Any",
      edu: "12th Pass / Graduate",
      minPay: 14000,
      maxPay: 20000,
      payType: "MONTHLY",
      vacancies: 2,
      desc: "Typing invoices, entering customer forms into Excel, scanning documents, and maintaining inventory records. Basic typing speed (25+ WPM) needed.",
      benefits: ["ESI & PF", "Fixed Office Hours", "Sunday Off"]
    }
  ]
};

module.exports = { TEMPLATES_BY_CATEGORY };
