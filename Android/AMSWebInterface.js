/* This plugin interfaces the webpage with Auxilium Mobile Solutions (Android App)
 */
window['mobile'] = {

	/* Check if interface is available
	 *
	 * return: boolean - interface present
	 */
	available: function(availableStatus) {
	    return window['AMS'] != null;
	},

	/* Open Walkie Talkie
	 *
	 * return: null
	 */
	openWalkieTalkie: function() {
		if(this.available()) AMS.openWalkieTalkie();
	},
	
	/* Create toast message on phone
	 *
	 * message: string - Message to display
	 * return: null
	 */
	createToast: function(message) {
		message = String(message);
		if(this.available() && AMS != 'ios' && message.length > 0) AMS.createToast(message);
	},

	/* Create notification on phone
	 *
	 * title: string - Title of notification (null for app name)
	 * body: string - What the notification says
	 * url: string - url to open when clicked (null to open users landing page)
	 * alarm: boolean - sound an alarm with notification
	 * return: null
	 */
	createNotification: function(title, body, url, alarm, gps) {
		title = String(title);
		if(this.available()) {
			if(alarm) this.soundAlarm();
			if(AMS == 'ios') {
                params = {"type": "createNotification", "title": title, "body": body,
                    "url": url || null, "alarm": alarm || false, "gps": gps || 0};
                location.href="auxmobile://"+JSON.stringify(params);
			} else {
            	AMS.createNotification(title, body, url || null, alarm || false, gps || 0);
			}
		}
	},
	
	/* TimePicker integration
	 *
	 *
	 *
	 */
	timePicker: function(){
	    AMS.showTimePicker();
	},

	/* Begin tracking GPS location of device for X minutes in Datalynk
	 *
	 * minutes: int - number of minutes to track phone (-1 for user default, RECOMENDED)
	 * return: null
	 */
	startTracking: function(minutes) {
		if(this.available()){
		    if(AMS == 'ios') {
		        var params = {"type": "startTracking", "time":minutes || -1};
                location.href="auxmobile://"+JSON.stringify(params);
		    } else {
		        AMS.startTracking(minutes || -1);
		    }
		}
	},

	/* Stop tracking
	 *
	 * return: null
	 */
	stopTracking: function() {
	    if(this.available()){
	        if(AMS == 'ios') {
	            var params = {"type": "stopTracking"};
                location.href="auxmobile://"+JSON.stringify(params);
	        } else {
	            AMS.stopTracking();
	        }
	    }
	},

    /* Is the app tracking location?
     *
     * return: boolean - tracking yes/no
     */
    isTracking: function(trackingStatus) {
        if(this.available) {
            if(AMS == 'ios') {
                var params = {"type": "isTracking"};
                location.href="auxmobile://"+JSON.stringify(params);
                if(trackingStatus == null) return 'start';
                return trackingStatus;
            } else {
                return AMS.isTracking();
            }
        }
    },
	/* Adjust media volume on phone
	 *
	 * volume: float - a float (0.0-1.0) representing phone volume as a percent
	 */
	setVolume: function(volume) {
		if(this.available() && volume >= 0 && volume <= 1){
		    if(AMS == 'ios') {
		        var params = {"type": "setVolume", "volume": volume};
                location.href="auxmobile://"+JSON.stringify(params);
		    } else {
		        AMS.setVolume(volume);
		    }
		}
	},

	/* Sound an alarm on the phone (No volume gaurentee)
	 *
	 * return: null
	 */
	soundAlarm: function() {
		if(this.available()){
		    if(AMS == 'ios') {
		        var params = {"type": "soundAlarm"};
                location.href="auxmobile://"+JSON.stringify(params);
		    } else {
		        AMS.soundAlarm();
		    }
		}
	},

    /* Add a callback to an event so that it runs when event is fired.
     *
     * event: string - event to listen for. (onPause, onResume, gpsStarted, gpsStopped)
     * return: null
     */
	addEventListener: function(event, callback) {
	    if(this.eventListeners == null) { this.eventListeners = {}; }
	    if(this.eventListeners[event] == null) { this.eventListeners[event] = []; }
	    this.eventListeners[event].push(callback);
	},

    /* Should only be used by AMS app. This fires events reflecting the state of the AMS app.
     *
     * event: string - event to fire
     * return: null
     */
	fireEvent: function(event) {
	    if(this.eventListeners != null && this.eventListeners[event] != null) {
	        this.eventListeners[event].forEach(function(value, index, array) {
	            value();
	        })
	    }
	}

}