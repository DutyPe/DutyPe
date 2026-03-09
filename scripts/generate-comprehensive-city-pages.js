#!/usr/bin/env node

/**
 * Script to generate comprehensive, SEO-optimized city job pages
 * Based on the enhanced Hyderabad template
 */

const fs = require('fs');
const path = require('path');

// City-specific data
const cityData = {
  'bangalore': {
    name: 'Bangalore',
    altName: 'Bengaluru',
    state: 'Karnataka',
    areas: ['Whitefield', 'Electronic City', 'Koramangala', 'Indiranagar', 'HSR Layout', 'Marathahalli', 'BTM Layout', 'Jayanagar', 'JP Nagar', 'Yelahanka'],
    specialties: ['IT capital', 'Garden city', 'Startup hub'],
    nearbyC