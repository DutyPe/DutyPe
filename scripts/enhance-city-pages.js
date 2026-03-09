const fs = require('fs');
const path = require('path');

// City data with specific information
const cities = {
  'hyderabad': {
    name: 'Hyderabad',
    state: 'Telangana',
    areas: ['Gachibowli', 'Madhapur', 'Hitech City', 'Banjara Hills', 'Jubilee Hills', 'Kondapur', 'Kukatpally', 'Miyapur', 'Ameerpet', 'Secunderabad'],
    specialties: ['IT hub', 'Biryani city', 'Pharma capital'],
    population: '10 million+',
    nearbyC