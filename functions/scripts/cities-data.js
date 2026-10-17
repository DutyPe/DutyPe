/**
 * Cities & Hubs Dataset for DutyPe Non-IT Job Postings
 */

const TELANGANA_LOCATIONS = [
  // Khammam (Major Focus - 15 Localities)
  { area: "Wyra Road, Khammam", addressText: "Beside Old Bus Stand, Wyra Road, Khammam, Telangana 507001", lat: 17.2510, lng: 80.1540, isKhammam: true },
  { area: "ZP Centre, Khammam", addressText: "Opposite Collectorate Complex, ZP Centre, Khammam, Telangana 507002", lat: 17.2473, lng: 80.1514, isKhammam: true },
  { area: "Bypass Road, Khammam", addressText: "Near Sri Sri Circle, Inner Bypass Road, Khammam, Telangana 507002", lat: 17.2380, lng: 80.1390, isKhammam: true },
  { area: "Rotary Nagar, Khammam", addressText: "Near Children Park, Rotary Nagar, Khammam, Telangana 507001", lat: 17.2435, lng: 80.1472, isKhammam: true },
  { area: "Mayuri Centre, Khammam", addressText: "Mayuri Centre, Main Commercial Hub, Khammam, Telangana 507001", lat: 17.2495, lng: 80.1488, isKhammam: true },
  { area: "Gandhi Chowk, Khammam", addressText: "Gandhi Chowk Bazaar Street, Khammam, Telangana 507003", lat: 17.2488, lng: 80.1555, isKhammam: true },
  { area: "Mamata Hospital Road, Khammam", addressText: "Near Mamata General Hospital, Rotary Nagar Road, Khammam, Telangana 507002", lat: 17.2625, lng: 80.1610, isKhammam: true },
  { area: "VDOs Colony, Khammam", addressText: "Plot 45, VDOs Colony Main Road, Khammam, Telangana 507002", lat: 17.2580, lng: 80.1450, isKhammam: true },
  { area: "Ricemill Road, Khammam", addressText: "Industrial Area, Ricemill Road, Khammam, Telangana 507003", lat: 17.2410, lng: 80.1620, isKhammam: true },
  { area: "Prakash Nagar, Khammam", addressText: "Near Municipal Park, Prakash Nagar, Khammam, Telangana 507001", lat: 17.2530, lng: 80.1500, isKhammam: true },
  { area: "Yellandu Cross Road, Khammam", addressText: "Yellandu Cross Road Junction, Khammam, Telangana 507002", lat: 17.2570, lng: 80.1650, isKhammam: true },
  { area: "NSP Camp, Khammam", addressText: "NSP Colony Grounds, Khammam, Telangana 507002", lat: 17.2400, lng: 80.1420, isKhammam: true },
  { area: "Kaviraj Nagar, Khammam", addressText: "Kaviraj Nagar Housing Area, Khammam, Telangana 507002", lat: 17.2610, lng: 80.1520, isKhammam: true },
  { area: "Danavaigudem, Khammam", addressText: "Danavaigudem Ring Road, Khammam, Telangana 507003", lat: 17.2350, lng: 80.1320, isKhammam: true },
  { area: "Gattaiah Centre, Khammam", addressText: "Gattaiah Centre Commercial Line, Khammam, Telangana 507002", lat: 17.2450, lng: 80.1530, isKhammam: true },

  // Rest of Telangana
  { area: "Kothagudem Main Road, Kothagudem", addressText: "Near Bus Station, Kothagudem, Bhadradri Kothagudem, Telangana 507101", lat: 17.5511, lng: 80.6175, isKhammam: false },
  { area: "Palvancha Centre, Palvancha", addressText: "Near KTPS Area, Palvancha, Bhadradri Kothagudem, Telangana 507115", lat: 17.5977, lng: 80.7061, isKhammam: false },
  { area: "Hanamkonda, Warangal", addressText: "Nakkalagutta, Hanamkonda, Warangal, Telangana 506001", lat: 17.9784, lng: 79.5941, isKhammam: false },
  { area: "Kazipet, Warangal", addressText: "Near Railway Station Road, Kazipet, Warangal, Telangana 506003", lat: 17.9818, lng: 79.5255, isKhammam: false },
  { area: "Suryapet Town, Suryapet", addressText: "Khammam Road, Suryapet, Telangana 508213", lat: 17.1439, lng: 79.6239, isKhammam: false },
  { area: "Clock Tower, Nalgonda", addressText: "Near Clock Tower Centre, Nalgonda, Telangana 508001", lat: 17.0575, lng: 79.2684, isKhammam: false },
  { area: "Collectorate Road, Karimnagar", addressText: "Kishan Nagar, Collectorate Road, Karimnagar, Telangana 505001", lat: 18.4386, lng: 79.1288, isKhammam: false },
  { area: "Khaleelwadi, Nizamabad", addressText: "Commercial Complex, Khaleelwadi, Nizamabad, Telangana 503001", lat: 18.6725, lng: 78.0941, isKhammam: false },
  { area: "Station Road, Mahabubnagar", addressText: "Near Railway Station, Mahabubnagar, Telangana 509001", lat: 16.7488, lng: 77.9856, isKhammam: false },
  { area: "Old Bus Stand, Siddipet", addressText: "Medak Road, Siddipet, Telangana 502103", lat: 18.1018, lng: 78.8520, isKhammam: false },
  { area: "Miryalaguda Town, Nalgonda", addressText: "Sagar Road, Miryalaguda, Nalgonda, Telangana 508207", lat: 16.8722, lng: 79.5631, isKhammam: false },
  { area: "Madhapur, Hyderabad", addressText: "Hitech City Main Road, Madhapur, Hyderabad, Telangana 500081", lat: 17.4483, lng: 78.3915, isKhammam: false },
  { area: "Kukatpally, Hyderabad", addressText: "KPHB Colony Phase 3, Kukatpally, Hyderabad, Telangana 500072", lat: 17.4849, lng: 78.4138, isKhammam: false },
  { area: "Secunderabad, Hyderabad", addressText: "MG Road, Secunderabad, Telangana 500003", lat: 17.4399, lng: 78.4983, isKhammam: false },
  { area: "Dilsukhnagar, Hyderabad", addressText: "Near Metro Pillar 1520, Dilsukhnagar, Hyderabad, Telangana 500060", lat: 17.3688, lng: 78.5247, isKhammam: false },
  { area: "Ameerpet, Hyderabad", addressText: "Opposite Mythrivanam, Ameerpet, Hyderabad, Telangana 500038", lat: 17.4375, lng: 78.4482, isKhammam: false },
  { area: "Gachibowli, Hyderabad", addressText: "ORR Junction, Gachibowli, Hyderabad, Telangana 500032", lat: 17.4401, lng: 78.3489, isKhammam: false },
  { area: "LB Nagar, Hyderabad", addressText: "Ring Road Circle, LB Nagar, Hyderabad, Telangana 500074", lat: 17.3457, lng: 78.5522, isKhammam: false },
  { area: "Uppal, Hyderabad", addressText: "Inner Ring Road, Uppal, Hyderabad, Telangana 500039", lat: 17.4018, lng: 78.5602, isKhammam: false },
  { area: "Miyapur, Hyderabad", addressText: "Allwyn X Road, Miyapur, Hyderabad, Telangana 500049", lat: 17.4968, lng: 78.3546, isKhammam: false }
];

const ANDHRA_PRADESH_LOCATIONS = [
  // Vijayawada
  { area: "Benz Circle, Vijayawada", addressText: "MG Road, Benz Circle, Vijayawada, Andhra Pradesh 520010", lat: 16.4975, lng: 80.6550 },
  { area: "Auto Nagar, Vijayawada", addressText: "Industrial Estate, Auto Nagar, Vijayawada, Andhra Pradesh 520007", lat: 16.5050, lng: 80.6780 },
  { area: "Governorpet, Vijayawada", addressText: "Prakasam Road, Governorpet, Vijayawada, Andhra Pradesh 520002", lat: 16.5120, lng: 80.6280 },
  { area: "One Town, Vijayawada", addressText: "Kaleswara Rao Market, One Town, Vijayawada, Andhra Pradesh 520001", lat: 16.5180, lng: 80.6120 },
  { area: "Gunadala, Vijayawada", addressText: "Eluru Road, Gunadala, Vijayawada, Andhra Pradesh 520004", lat: 16.5240, lng: 80.6650 },
  { area: "Bhavanipuram, Vijayawada", addressText: "HB Colony, Bhavanipuram, Vijayawada, Andhra Pradesh 520012", lat: 16.5290, lng: 80.5980 },
  { area: "Moghalrajpuram, Vijayawada", addressText: "Siddhartha College Road, Moghalrajpuram, Vijayawada, Andhra Pradesh 520010", lat: 16.5020, lng: 80.6420 },
  { area: "Kanuru, Vijayawada", addressText: "Near VR Siddhartha College, Kanuru, Vijayawada, Andhra Pradesh 520007", lat: 16.4880, lng: 80.6920 },
  { area: "Poranki, Vijayawada", addressText: "Bandar Road, Poranki, Vijayawada, Andhra Pradesh 521137", lat: 16.4790, lng: 80.7120 },
  { area: "Gollapudi, Vijayawada", addressText: "Bypass Road, Gollapudi, Vijayawada, Andhra Pradesh 521225", lat: 16.5410, lng: 80.5780 },

  // Guntur
  { area: "Arundelpet, Guntur", addressText: "Main Road, Arundelpet, Guntur, Andhra Pradesh 522002", lat: 16.3020, lng: 80.4410 },
  { area: "Brodipet, Guntur", addressText: "4th Line, Brodipet, Guntur, Andhra Pradesh 522002", lat: 16.3080, lng: 80.4460 },
  { area: "Lakshmipuram, Guntur", addressText: "Near Naaz Centre, Lakshmipuram, Guntur, Andhra Pradesh 522007", lat: 16.2950, lng: 80.4350 },
  { area: "Koretipadu, Guntur", addressText: "Koretipadu Main Road, Guntur, Andhra Pradesh 522007", lat: 16.3150, lng: 80.4300 },
  { area: "Pattabhipuram, Guntur", addressText: "Pattabhipuram Main Road, Guntur, Andhra Pradesh 522006", lat: 16.2910, lng: 80.4280 },
  { area: "Auto Nagar, Guntur", addressText: "Phase 1 Industrial Area, Auto Nagar, Guntur, Andhra Pradesh 522001", lat: 16.3250, lng: 80.4700 },

  // Visakhapatnam
  { area: "Gajuwaka, Visakhapatnam", addressText: "High School Road, Gajuwaka, Visakhapatnam, Andhra Pradesh 530026", lat: 17.6905, lng: 83.2084 },
  { area: "Madhurawada, Visakhapatnam", addressText: "Near IT SEZ, Madhurawada, Visakhapatnam, Andhra Pradesh 530048", lat: 17.8180, lng: 83.3520 },
  { area: "Dwaraka Nagar, Visakhapatnam", addressText: "2nd Lane, Dwaraka Nagar, Visakhapatnam, Andhra Pradesh 530016", lat: 17.7280, lng: 83.3050 },
  { area: "MVP Colony, Visakhapatnam", addressText: "Sector 3, MVP Colony, Visakhapatnam, Andhra Pradesh 530017", lat: 17.7410, lng: 83.3320 },
  { area: "Siripuram, Visakhapatnam", addressText: "Siripuram Junction, Visakhapatnam, Andhra Pradesh 530003", lat: 17.7220, lng: 83.3180 },
  { area: "Pendurthi, Visakhapatnam", addressText: "Near Railway Station, Pendurthi, Visakhapatnam, Andhra Pradesh 531173", lat: 17.8320, lng: 83.2010 },
  { area: "Kurmannapalem, Visakhapatnam", addressText: "Steel Plant Gate, Kurmannapalem, Visakhapatnam, Andhra Pradesh 530046", lat: 17.6740, lng: 83.1680 },
  { area: "NAD Junction, Visakhapatnam", addressText: "NAD Kotha Road, Visakhapatnam, Andhra Pradesh 530027", lat: 17.7450, lng: 83.2210 },

  // Tirupati
  { area: "Bhavani Nagar, Tirupati", addressText: "Near Railway Station, Bhavani Nagar, Tirupati, Andhra Pradesh 517501", lat: 13.6288, lng: 79.4192 },
  { area: "Renigunta Road, Tirupati", addressText: "Opposite APSRTC Central Bus Stand, Renigunta Road, Tirupati, Andhra Pradesh 517501", lat: 13.6350, lng: 79.4380 },
  { area: "Korlagunta, Tirupati", addressText: "Main Road, Korlagunta, Tirupati, Andhra Pradesh 517501", lat: 13.6390, lng: 79.4210 },
  { area: "AIR Bypass Road, Tirupati", addressText: "Beside D-Mart, AIR Bypass Road, Tirupati, Andhra Pradesh 517501", lat: 13.6210, lng: 79.4080 },

  // Rajahmundry
  { area: "Danavaipeta, Rajahmundry", addressText: "Main Road, Danavaipeta, Rajahmundry, Andhra Pradesh 533103", lat: 17.0050, lng: 81.7820 },
  { area: "Morampudi, Rajahmundry", addressText: "Morampudi Junction, Rajahmundry, Andhra Pradesh 533107", lat: 17.0180, lng: 81.8020 },
  { area: "Kotipalli, Rajahmundry", addressText: "Near Kotipalli Bus Stand, Rajahmundry, Andhra Pradesh 533101", lat: 16.9950, lng: 81.7750 },

  // Nellore
  { area: "Dargamitta, Nellore", addressText: "Collectorate Road, Dargamitta, Nellore, Andhra Pradesh 524003", lat: 14.4426, lng: 79.9865 },
  { area: "Magunta Layout, Nellore", addressText: "Main Road, Magunta Layout, Nellore, Andhra Pradesh 524003", lat: 14.4350, lng: 79.9720 },
  { area: "Trunk Road, Nellore", addressText: "Trunk Road Bazaar, Nellore, Andhra Pradesh 524001", lat: 14.4490, lng: 79.9880 },

  // Kurnool
  { area: "Budhwarpet, Kurnool", addressText: "Near Collector Office, Budhwarpet, Kurnool, Andhra Pradesh 518002", lat: 15.8281, lng: 78.0373 },
  { area: "C.Camp, Kurnool", addressText: "Main Circle, C.Camp, Kurnool, Andhra Pradesh 518002", lat: 15.8350, lng: 78.0480 },
  { area: "Gayatri Estate, Kurnool", addressText: "Gayatri Estate, Kurnool, Andhra Pradesh 518003", lat: 15.8190, lng: 78.0280 },

  // Kakinada
  { area: "Main Road, Kakinada", addressText: "Near Cinema Road, Main Road, Kakinada, Andhra Pradesh 533001", lat: 16.9891, lng: 82.2475 },
  { area: "Bhanugudi, Kakinada", addressText: "Bhanugudi Junction, Kakinada, Andhra Pradesh 533003", lat: 16.9750, lng: 82.2380 },
  { area: "Sarpavaram, Kakinada", addressText: "Sarpavaram Junction, Kakinada, Andhra Pradesh 533005", lat: 17.0120, lng: 82.2580 },

  // Anantapur
  { area: "Subash Road, Anantapur", addressText: "Opposite Clock Tower, Subash Road, Anantapur, Andhra Pradesh 515001", lat: 14.6819, lng: 77.6006 },
  { area: "Housing Board, Anantapur", addressText: "Near Court Complex, Housing Board Colony, Anantapur, Andhra Pradesh 515002", lat: 14.6720, lng: 77.5910 },

  // Other AP Districts
  { area: "Powerpet, Eluru", addressText: "Near Powerpet Railway Station, Eluru, Andhra Pradesh 534002", lat: 16.7107, lng: 81.0952 },
  { area: "Nagarajupalli, Kadapa", addressText: "Near RIMS Hospital Road, Nagarajupalli, Kadapa, Andhra Pradesh 516001", lat: 14.4673, lng: 78.8242 },
  { area: "Kurnool Road, Ongole", addressText: "Near Collector Office, Kurnool Road, Ongole, Andhra Pradesh 523002", lat: 15.5057, lng: 80.0499 },
  { area: "Seven Hills, Srikakulam", addressText: "Day and Night Junction, Seven Hills, Srikakulam, Andhra Pradesh 532001", lat: 18.2949, lng: 83.8938 },
  { area: "Cantonment, Vizianagaram", addressText: "Collectorate Road, Cantonment, Vizianagaram, Andhra Pradesh 535003", lat: 18.1133, lng: 83.4072 },
  { area: "Kongareddy Palli, Chittoor", addressText: "Near High School Road, Kongareddy Palli, Chittoor, Andhra Pradesh 517001", lat: 13.2172, lng: 79.1003 },
  { area: "Someswaram, Bhimavaram", addressText: "Near Someswara Temple Road, Bhimavaram, Andhra Pradesh 534201", lat: 16.5449, lng: 81.5212 },
  { area: "Bose Road, Tenali", addressText: "Bose Road Main Commercial, Tenali, Guntur, Andhra Pradesh 522201", lat: 16.2437, lng: 80.6406 },
  { area: "Rustumbada, Machilipatnam", addressText: "Near Bus Station, Rustumbada, Machilipatnam, Andhra Pradesh 521001", lat: 16.1875, lng: 81.1389 },
  { area: "Srinivasa Nagar, Nandyal", addressText: "Atmakur Road, Srinivasa Nagar, Nandyal, Andhra Pradesh 518501", lat: 15.4870, lng: 78.4830 },
  { area: "Palnadu Road, Narasaraopet", addressText: "Near Clock Tower, Palnadu Road, Narasaraopet, Andhra Pradesh 522601", lat: 16.2360, lng: 80.0520 },
  { area: "Cinema Road, Proddatur", addressText: "Mydukur Road, Proddatur, Kadapa, Andhra Pradesh 516360", lat: 14.7500, lng: 78.5500 }
];

const KERALA_LOCATIONS = [
  { area: "Ernakulam South, Kochi", addressText: "Near South Railway Station, Ernakulam, Kochi, Kerala 682016", lat: 9.9674, lng: 76.2917 },
  { area: "Edappally, Kochi", addressText: "Lulu Mall Road, Edappally, Kochi, Kerala 682024", lat: 10.0261, lng: 76.3125 },
  { area: "Kakkanad, Kochi", addressText: "Near InfoPark Gate 1, Kakkanad, Kochi, Kerala 682030", lat: 10.0159, lng: 76.3419 },
  { area: "Kaloor, Kochi", addressText: "Near Kaloor Stadium, Kaloor, Kochi, Kerala 682017", lat: 9.9984, lng: 76.2996 },
  { area: "Fort Kochi, Kochi", addressText: "Princess Street, Fort Kochi, Kerala 682001", lat: 9.9658, lng: 76.2421 },
  { area: "Vyttila, Kochi", addressText: "Near Mobility Hub, Vyttila, Kochi, Kerala 682019", lat: 9.9664, lng: 76.3197 },
  { area: "Aluva, Kochi", addressText: "Near Private Bus Stand, Aluva, Ernakulam, Kerala 683101", lat: 10.1076, lng: 76.3516 },
  { area: "Pattom, Thiruvananthapuram", addressText: "Pattom Palace Road, Thiruvananthapuram, Kerala 695004", lat: 8.5241, lng: 76.9366 },
  { area: "Kazhakoottam, Thiruvananthapuram", addressText: "Technopark Phase 1, Kazhakoottam, Thiruvananthapuram, Kerala 695582", lat: 8.5686, lng: 76.8731 },
  { area: "Thampanoor, Thiruvananthapuram", addressText: "Near Central Railway Station, Thampanoor, Thiruvananthapuram, Kerala 695001", lat: 8.4900, lng: 76.9530 },
  { area: "Kowdiar, Thiruvananthapuram", addressText: "Kowdiar Main Road, Thiruvananthapuram, Kerala 695003", lat: 8.5255, lng: 76.9620 },
  { area: "Mavoor Road, Kozhikode", addressText: "Opposite KSRTC Bus Terminal, Mavoor Road, Kozhikode, Kerala 673004", lat: 11.2588, lng: 75.7804 },
  { area: "Palayam, Kozhikode", addressText: "Near Palayam Market, Kozhikode, Kerala 673002", lat: 11.2520, lng: 75.7880 },
  { area: "Round North, Thrissur", addressText: "Swaraj Round North, Thrissur, Kerala 680001", lat: 10.5276, lng: 76.2144 },
  { area: "Ayyanthole, Thrissur", addressText: "Civil Station Road, Ayyanthole, Thrissur, Kerala 680003", lat: 10.5340, lng: 76.1980 },
  { area: "Chinnakada, Kollam", addressText: "Chinnakada Clock Tower Centre, Kollam, Kerala 691001", lat: 8.8872, lng: 76.5956 },
  { area: "Stadium Bypass, Palakkad", addressText: "Near Municipal Stadium, Stadium Bypass, Palakkad, Kerala 678001", lat: 10.7867, lng: 76.6548 },
  { area: "Thavakkara, Kannur", addressText: "Near New Bus Stand, Thavakkara, Kannur, Kerala 670001", lat: 11.8745, lng: 75.3704 },
  { area: "Mullakkal, Alappuzha", addressText: "Mullakkal Temple Road, Alappuzha, Kerala 688011", lat: 9.4981, lng: 76.3388 },
  { area: "Kanjikuzhy, Kottayam", addressText: "KK Road, Kanjikuzhy, Kottayam, Kerala 686004", lat: 9.5850, lng: 76.5380 }
];

const TAMIL_NADU_LOCATIONS = [
  // Chennai
  { area: "T. Nagar, Chennai", addressText: "Pondy Bazaar, T. Nagar, Chennai, Tamil Nadu 600017", lat: 13.0418, lng: 80.2341 },
  { area: "Anna Nagar, Chennai", addressText: "2nd Avenue, Anna Nagar Roundtana, Chennai, Tamil Nadu 600040", lat: 13.0850, lng: 80.2101 },
  { area: "Velachery, Chennai", addressText: "100 Feet Bypass Road, Velachery, Chennai, Tamil Nadu 600042", lat: 12.9759, lng: 80.2212 },
  { area: "Guindy, Chennai", addressText: "Guindy Industrial Estate, Chennai, Tamil Nadu 600032", lat: 13.0067, lng: 80.2025 },
  { area: "Tambaram, Chennai", addressText: "GST Road, Tambaram West, Chennai, Tamil Nadu 600045", lat: 12.9249, lng: 80.1000 },
  { area: "OMR Sholinganallur, Chennai", addressText: "IT Expressway, Sholinganallur, Chennai, Tamil Nadu 600119", lat: 12.9010, lng: 80.2279 },
  { area: "Porur, Chennai", addressText: "Arcot Road, Porur Junction, Chennai, Tamil Nadu 600116", lat: 13.0382, lng: 80.1565 },
  { area: "Ambattur, Chennai", addressText: "Ambattur Industrial Estate, Chennai, Tamil Nadu 600058", lat: 13.1143, lng: 80.1548 },
  { area: "Vadapalani, Chennai", addressText: "Near Forum Vijaya Mall, Vadapalani, Chennai, Tamil Nadu 600026", lat: 13.0500, lng: 80.2120 },
  { area: "Adyar, Chennai", addressText: "LB Road, Adyar, Chennai, Tamil Nadu 600020", lat: 13.0012, lng: 80.2565 },
  { area: "Mylapore, Chennai", addressText: "Luz Church Road, Mylapore, Chennai, Tamil Nadu 600004", lat: 13.0368, lng: 80.2676 },

  // Coimbatore
  { area: "Gandhipuram, Coimbatore", addressText: "Cross Cut Road, Gandhipuram, Coimbatore, Tamil Nadu 641012", lat: 11.0168, lng: 76.9673 },
  { area: "RS Puram, Coimbatore", addressText: "DB Road, RS Puram, Coimbatore, Tamil Nadu 641002", lat: 11.0084, lng: 76.9525 },
  { area: "Peelamedu, Coimbatore", addressText: "Avinashi Road, Peelamedu, Coimbatore, Tamil Nadu 641004", lat: 11.0289, lng: 77.0125 },
  { area: "Saravanampatti, Coimbatore", addressText: "Sathy Road, Saravanampatti, Coimbatore, Tamil Nadu 641035", lat: 11.0797, lng: 76.9997 },
  { area: "Singanallur, Coimbatore", addressText: "Trichy Road, Singanallur, Coimbatore, Tamil Nadu 641005", lat: 10.9990, lng: 77.0250 },

  // Madurai
  { area: "Simmakkal, Madurai", addressText: "Vakkil New Street, Simmakkal, Madurai, Tamil Nadu 625001", lat: 9.9252, lng: 78.1198 },
  { area: "Anna Nagar, Madurai", addressText: "Main Road, Anna Nagar, Madurai, Tamil Nadu 625020", lat: 9.9180, lng: 78.1480 },
  { area: "KK Nagar, Madurai", addressText: "80 Feet Road, KK Nagar, Madurai, Tamil Nadu 625020", lat: 9.9320, lng: 78.1520 },

  // Tiruchirappalli
  { area: "Thillai Nagar, Tiruchirappalli", addressText: "Main Road, Thillai Nagar, Tiruchirappalli, Tamil Nadu 620018", lat: 10.8286, lng: 78.6865 },
  { area: "Cantonment, Tiruchirappalli", addressText: "Near Central Bus Stand, Cantonment, Tiruchirappalli, Tamil Nadu 620001", lat: 10.8050, lng: 78.6820 },

  // Salem & Others
  { area: "Fairlands, Salem", addressText: "Brindavan Road, Fairlands, Salem, Tamil Nadu 636016", lat: 11.6643, lng: 78.1460 },
  { area: "Avinashi Road, Tiruppur", addressText: "Pushpa Theatre Junction, Avinashi Road, Tiruppur, Tamil Nadu 641602", lat: 11.1085, lng: 77.3411 },
  { area: "Perundurai Road, Erode", addressText: "Collectorate Office Road, Perundurai Road, Erode, Tamil Nadu 638011", lat: 11.3410, lng: 77.7172 },
  { area: "Katpadi, Vellore", addressText: "Near VIT Campus, Katpadi, Vellore, Tamil Nadu 632014", lat: 12.9698, lng: 79.1378 },
  { area: "Palayamkottai, Tirunelveli", addressText: "High Ground Road, Palayamkottai, Tirunelveli, Tamil Nadu 627002", lat: 8.7139, lng: 77.7567 }
];

const DELHI_NCR_LOCATIONS = [
  { area: "Connaught Place, New Delhi", addressText: "Outer Circle, Block E, Connaught Place, New Delhi 110001", lat: 28.6315, lng: 77.2167 },
  { area: "Karol Bagh, New Delhi", addressText: "Padam Singh Road, Karol Bagh, New Delhi 110005", lat: 28.6514, lng: 77.1907 },
  { area: "Laxmi Nagar, New Delhi", addressText: "Vikas Marg, Near Metro Station, Laxmi Nagar, New Delhi 110092", lat: 28.6304, lng: 77.2773 },
  { area: "Rohini Sector 7, New Delhi", addressText: "Naharpur Road, Sector 7, Rohini, New Delhi 110085", lat: 28.7166, lng: 77.1189 },
  { area: "Dwarka Sector 10, New Delhi", addressText: "Market Complex, Sector 10, Dwarka, New Delhi 110075", lat: 28.5823, lng: 77.0500 },
  { area: "Lajpat Nagar Central Market, New Delhi", addressText: "Central Market, Lajpat Nagar II, New Delhi 110024", lat: 28.5700, lng: 77.2400 },
  { area: "Okhla Phase 2, New Delhi", addressText: "Industrial Area, Okhla Phase II, New Delhi 110020", lat: 28.5355, lng: 77.2711 },
  { area: "Chandni Chowk, New Delhi", addressText: "Fatehpuri Bazaar, Chandni Chowk, New Delhi 110006", lat: 28.6506, lng: 77.2303 },
  { area: "Pitampura, New Delhi", addressText: "Netaji Subhash Place Complex, Pitampura, New Delhi 110034", lat: 28.6990, lng: 77.1384 },
  { area: "Janakpuri, New Delhi", addressText: "District Centre, Janakpuri, New Delhi 110058", lat: 28.6219, lng: 77.0878 },
  { area: "Saket, New Delhi", addressText: "Press Enclave Road, Saket, New Delhi 110017", lat: 28.5244, lng: 77.2173 },
  { area: "Nehru Place, New Delhi", addressText: "Commercial Complex, Nehru Place, New Delhi 110019", lat: 28.5494, lng: 77.2528 },
  { area: "Rajouri Garden, New Delhi", addressText: "Main Market, Rajouri Garden, New Delhi 110027", lat: 28.6470, lng: 77.1210 },
  { area: "Shahdara, New Delhi", addressText: "Grand Trunk Road, Shahdara, New Delhi 110032", lat: 28.6734, lng: 77.2882 },
  { area: "Mayur Vihar, New Delhi", addressText: "Phase 1 Pocket 1, Mayur Vihar, New Delhi 110091", lat: 28.6083, lng: 77.2950 },

  // Noida & Gurgaon
  { area: "Sector 18, Noida", addressText: "Atta Market, Sector 18, Noida, Uttar Pradesh 201301", lat: 28.5708, lng: 77.3261 },
  { area: "Sector 62, Noida", addressText: "Institutional Area, Sector 62, Noida, Uttar Pradesh 201309", lat: 28.6270, lng: 77.3725 },
  { area: "Sector 63, Noida", addressText: "Electronic City, Sector 63, Noida, Uttar Pradesh 201301", lat: 28.6240, lng: 77.3850 },
  { area: "Cyber City, Gurgaon", addressText: "DLF Cyber City, Phase 2, Gurgaon, Haryana 122002", lat: 28.4947, lng: 77.0888 },
  { area: "Sector 29, Gurgaon", addressText: "Commercial Leisure Valley, Sector 29, Gurgaon, Haryana 122001", lat: 28.4682, lng: 77.0635 },
  { area: "Sohna Road, Gurgaon", addressText: "Subhash Chowk, Sohna Road, Gurgaon, Haryana 122018", lat: 28.4150, lng: 77.0420 }
];

module.exports = {
  TELANGANA_LOCATIONS,
  ANDHRA_PRADESH_LOCATIONS,
  KERALA_LOCATIONS,
  TAMIL_NADU_LOCATIONS,
  DELHI_NCR_LOCATIONS
};
