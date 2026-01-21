package com.example.dutype.worker.screens.map

/**
 * Generates the HTML content for Azure Maps WebView
 * Uses Azure Maps JavaScript SDK for full map functionality
 */
fun generateAzureMapHtml(
    azureKey: String,
    centerLat: Double,
    centerLng: Double,
    zoom: Int
): String = """
<!DOCTYPE html>
<html>
<head>
    <meta charset="utf-8">
    <meta name="viewport" content="width=device-width, initial-scale=1.0, maximum-scale=1.0, user-scalable=no">
    <title>Job Map</title>
    
    <!-- Azure Maps CSS -->
    <link rel="stylesheet" href="https://atlas.microsoft.com/sdk/javascript/mapcontrol/3/atlas.min.css" type="text/css">
    
    <style>
        html, body {
            margin: 0;
            padding: 0;
            width: 100%;
            height: 100%;
            overflow: hidden;
            background: #f0f0f0;
        }
        #map {
            width: 100%;
            height: 100%;
        }
        #error {
            display: none;
            position: absolute;
            top: 50%;
            left: 50%;
            transform: translate(-50%, -50%);
            text-align: center;
            padding: 20px;
            background: white;
            border-radius: 10px;
            box-shadow: 0 2px 10px rgba(0,0,0,0.2);
        }
        
        /* Job marker pin styles */
        .job-marker {
            cursor: pointer;
            transition: transform 0.2s;
        }
        .job-marker:hover {
            transform: scale(1.1);
        }
        .job-pin {
            width: 30px;
            height: 40px;
            position: relative;
        }
        .job-pin-head {
            width: 30px;
            height: 30px;
            border-radius: 50% 50% 50% 0;
            transform: rotate(-45deg);
            display: flex;
            align-items: center;
            justify-content: center;
            box-shadow: 0 2px 6px rgba(0,0,0,0.3);
        }
        .job-pin-head.available {
            background: linear-gradient(135deg, #10B981, #059669);
        }
        .job-pin-head.urgent {
            background: linear-gradient(135deg, #EF4444, #DC2626);
        }
        .job-pin-icon {
            transform: rotate(45deg);
            font-size: 14px;
        }
        
        /* Popup styling */
        .popup-content {
            padding: 12px;
            max-width: 220px;
            font-family: -apple-system, BlinkMacSystemFont, 'Segoe UI', Roboto, sans-serif;
        }
        .popup-title {
            font-weight: 600;
            font-size: 14px;
            color: #1E293B;
            margin-bottom: 4px;
        }
        .popup-company {
            font-size: 12px;
            color: #6B7280;
            margin-bottom: 6px;
        }
        .popup-pay {
            font-size: 13px;
            color: #059669;
            font-weight: 600;
        }
        .popup-urgent {
            background: #FEE2E2;
            color: #DC2626;
            padding: 2px 8px;
            border-radius: 4px;
            font-size: 10px;
            font-weight: bold;
            display: inline-block;
            margin-bottom: 6px;
        }
    </style>
</head>
<body>
    <div id="loading" style="position: absolute; top: 50%; left: 50%; transform: translate(-50%, -50%); text-align: center; z-index: 1000;">
        <div style="width: 40px; height: 40px; border: 4px solid #f3f3f3; border-top: 4px solid #2563EB; border-radius: 50%; animation: spin 1s linear infinite; margin: 0 auto;"></div>
        <p style="margin-top: 10px; color: #666;">Loading map...</p>
    </div>
    <style>
        @keyframes spin {
            0% { transform: rotate(0deg); }
            100% { transform: rotate(360deg); }
        }
    </style>
    <div id="map"></div>
    <div id="error">
        <p style="font-size: 24px;">⚠️</p>
        <p style="font-weight: bold; margin: 10px 0;">Map loading failed</p>
        <p id="errorMsg" style="color: #666; font-size: 12px;"></p>
        <button onclick="initMap()" style="margin-top: 10px; padding: 8px 16px; background: #2563EB; color: white; border: none; border-radius: 8px; cursor: pointer;">Retry</button>
    </div>
    
    <!-- Azure Maps JavaScript SDK -->
    <script src="https://atlas.microsoft.com/sdk/javascript/mapcontrol/3/atlas.min.js"></script>
    
    <script>
        var map = null;
        var jobMarkers = [];
        var userMarker = null;
        var popup = null;
        var mapReady = false;
        
        // Initialize map when page loads
        document.addEventListener('DOMContentLoaded', function() {
            setTimeout(initMap, 100);
        });
        
        function showError(msg) {
            var loadingEl = document.getElementById('loading');
            if (loadingEl) loadingEl.style.display = 'none';
            document.getElementById('error').style.display = 'block';
            document.getElementById('errorMsg').textContent = msg;
            console.error('Map Error:', msg);
        }
        
        function initMap() {
            try {
                console.log('Initializing Azure Maps...');
                
                // Create map instance
                map = new atlas.Map('map', {
                    center: [$centerLng, $centerLat],
                    zoom: $zoom,
                    language: 'en-US',
                    authOptions: {
                        authType: 'subscriptionKey',
                        subscriptionKey: '$azureKey'
                    },
                    style: 'road',
                    showFeedbackLink: false,
                    showLogo: true
                });
                
                // Handle map errors
                map.events.add('error', function(e) {
                    console.error('Map error event:', e);
                    showError(e.error ? e.error.message : 'Unknown error');
                });
                
                // Wait for map to be ready
                map.events.add('ready', function() {
                    console.log('Azure Maps ready!');
                    mapReady = true;
                    
                    // Hide loading indicator
                    var loadingEl = document.getElementById('loading');
                    if (loadingEl) loadingEl.style.display = 'none';
                    
                    // Create popup
                    popup = new atlas.Popup({
                        pixelOffset: [0, -45],
                        closeButton: true
                    });
                    
                    // Notify Android that map is ready
                    if (window.Android) {
                        try {
                            Android.onMapReady();
                        } catch(e) {
                            console.log('Android callback error:', e);
                        }
                    }
                });
                
            } catch (error) {
                console.error('Map initialization error:', error);
                showError(error.message || 'Failed to initialize map');
            }
        }
        
        // Create HTML for job marker pin
        function createJobMarkerHtml(isUrgent) {
            var colorClass = isUrgent ? 'urgent' : 'available';
            var icon = isUrgent ? '🔥' : '💼';
            return '<div class="job-marker">' +
                '<div class="job-pin">' +
                '<div class="job-pin-head ' + colorClass + '">' +
                '<span class="job-pin-icon">' + icon + '</span>' +
                '</div>' +
                '</div>' +
                '</div>';
        }
        
        // Update markers on the map
        function updateMarkers(jobsData) {
            console.log('updateMarkers called with', jobsData ? jobsData.length : 0, 'jobs');
            
            if (!map || !mapReady) {
                console.log('Map not ready, retrying in 500ms...');
                setTimeout(function() { updateMarkers(jobsData); }, 500);
                return;
            }
            
            // Clear existing job markers
            jobMarkers.forEach(function(marker) {
                map.markers.remove(marker);
            });
            jobMarkers = [];
            
            if (!jobsData || jobsData.length === 0) {
                console.log('No jobs to display');
                return;
            }
            
            // Add new markers
            jobsData.forEach(function(job) {
                try {
                    var marker = new atlas.HtmlMarker({
                        position: [job.lng, job.lat],
                        htmlContent: createJobMarkerHtml(job.urgent),
                        anchor: 'bottom'
                    });
                    
                    // Store job data on marker
                    marker.jobData = job;
                    
                    // Add click event
                    map.events.add('click', marker, function() {
                        showJobPopup(marker);
                        
                        // Notify Android
                        if (window.Android) {
                            try {
                                Android.onMarkerClick(job.id);
                            } catch(e) {
                                console.log('Android callback error:', e);
                            }
                        }
                    });
                    
                    map.markers.add(marker);
                    jobMarkers.push(marker);
                    console.log('Added marker for job:', job.title, 'at', job.lat, job.lng);
                } catch(e) {
                    console.error('Error adding marker:', e);
                }
            });
            
            console.log('Total markers added:', jobMarkers.length);
        }
        
        // Show popup for a job marker
        function showJobPopup(marker) {
            if (!popup || !marker.jobData) return;
            
            var job = marker.jobData;
            var urgentBadge = job.urgent ? 
                '<div class="popup-urgent">🔥 URGENT</div>' : '';
            
            var content = '<div class="popup-content">' +
                urgentBadge +
                '<div class="popup-title">' + escapeHtml(job.title) + '</div>' +
                '<div class="popup-company">' + escapeHtml(job.companyName) + '</div>' +
                '<div class="popup-pay">💰 ' + escapeHtml(job.pay || 'Negotiable') + '</div>' +
                '</div>';
            
            popup.setOptions({
                content: content,
                position: marker.getOptions().position
            });
            
            popup.open(map);
        }
        
        // Update user location marker
        function updateUserLocation(lat, lng) {
            console.log('updateUserLocation called:', lat, lng);
            
            if (!map || !mapReady) {
                console.log('Map not ready for user location');
                return;
            }
            
            // Remove existing user marker
            if (userMarker) {
                map.markers.remove(userMarker);
            }
            
            // Create user location marker with blue dot
            var userHtml = '<div style="' +
                'width: 20px; height: 20px; ' +
                'background: #2563EB; ' +
                'border: 3px solid white; ' +
                'border-radius: 50%; ' +
                'box-shadow: 0 2px 6px rgba(0,0,0,0.3);' +
                '"></div>';
            
            userMarker = new atlas.HtmlMarker({
                position: [lng, lat],
                htmlContent: userHtml,
                anchor: 'center'
            });
            
            map.markers.add(userMarker);
            console.log('User marker added');
        }
        
        // Center map on location
        function centerMap(lat, lng, zoom) {
            console.log('centerMap called:', lat, lng, zoom);
            if (!map) return;
            
            map.setCamera({
                center: [lng, lat],
                zoom: zoom || 15,
                type: 'ease',
                duration: 500
            });
        }
        
        // Zoom in
        function zoomIn() {
            if (!map) return;
            var currentZoom = map.getCamera().zoom;
            map.setCamera({
                zoom: Math.min(currentZoom + 1, 20),
                type: 'ease',
                duration: 300
            });
        }
        
        // Zoom out
        function zoomOut() {
            if (!map) return;
            var currentZoom = map.getCamera().zoom;
            map.setCamera({
                zoom: Math.max(currentZoom - 1, 1),
                type: 'ease',
                duration: 300
            });
        }
        
        // Helper function to escape HTML
        function escapeHtml(text) {
            if (!text) return '';
            var div = document.createElement('div');
            div.textContent = text;
            return div.innerHTML;
        }
    </script>
</body>
</html>
""".trimIndent()
