#!/bin/bash

# Script to update canonical URLs in all city pages from dutypein.web.app to dutype.in

echo "Updating canonical URLs in all city job pages..."

# List of city pages
cities=(
  "bangalore"
  "mumbai"
  "delhi"
  "vijayawada"
  "tirupati"
  "warangal"
  "guntur"
  "kakinada"
  "nellore"
  "nizamabad"
  "karimnagar"
  "rajahmundry"
  "anantapur"
  "khammam"
)

for city in "${cities[@]}"; do
  file="public/jobs-in-${city}.html"
  if [ -f "$file" ]; then
    echo "Updating $file..."
    sed -i 's/dutypein\.web\.app/dutype.in/g' "$file"
  fi
done

echo "All city pages updated!"
echo "Run 'firebase deploy --only hosting:dutype-860ac' to deploy changes"
