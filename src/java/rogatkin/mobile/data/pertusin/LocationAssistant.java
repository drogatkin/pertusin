package rogatkin.mobile.data.pertusin;

import android.content.Context;
import android.content.pm.PackageManager;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.Build;
import android.os.Bundle;
import android.util.Log;

public class LocationAssistant {

	private static final String TAG = "pertusin-location";
	@InjectA
	Context context;

	public interface LocationConsumer {
		void locationAquired(Location location);
	}

	public LocationAssistant(Context ctx) {
		context = ctx;
	}

	/** request current location, when location detected a listener will be called
	 * 
	 * @param accuracy
	 * @param maxWaitMs
	 * @param lc
	 * @return
	 */
	public boolean requestLocation(float accuracy, final int maxWaitMs, final LocationConsumer lc) {
		if (!isPermitted()) {
			if (!askLocationPermission())
				return false;
		}
		final LocationManager locationManager = (LocationManager) context.getSystemService(Context.LOCATION_SERVICE);
		String provider = LocationManager.NETWORK_PROVIDER;
		if (maxWaitMs < 500) { //just return last known
			lc.locationAquired(getLocation());
			return true;
		}
		final long startTime = System.currentTimeMillis();
		LocationListener locationListener = new LocationListener() {

			Location location;

			public void onLocationChanged(Location loc) {
				boolean stop = System.currentTimeMillis() - startTime >= maxWaitMs;
				if (location == null)
					location = loc;
				else {
					if (getBetterLocation(location, loc) == loc) {
						stop = true;
					}
				}
				if (stop) {
					locationManager.removeUpdates(this);
					lc.locationAquired(loc);
				}
			}

			public void onProviderDisabled(String provider) {
				// TODO Auto-generated method stub

			}

			public void onProviderEnabled(String provider) {
				// TODO Auto-generated method stub

			}

			public void onStatusChanged(String provider, int status, Bundle extras) {
				// TODO Auto-generated method stub

			}

		};

		try {

			if (LocationManager.GPS_PROVIDER.equals(provider)) {
				locationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER, 0, 0, locationListener);
			} else if (LocationManager.NETWORK_PROVIDER.equals(provider)) {
				locationManager.requestLocationUpdates(LocationManager.NETWORK_PROVIDER, 0, 0, locationListener);
			} else {
				locationManager.requestLocationUpdates(LocationManager.NETWORK_PROVIDER, 0, 0, locationListener);
				locationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER, 0, 0, locationListener);
			}
		} catch (Exception e) {
			Log.e(TAG, e.getMessage());
			return false;
		}

		return true;
	}

	protected boolean isPermitted() {
		if (Build.VERSION.SDK_INT >= 23
				&& context.checkCallingOrSelfPermission(
						android.Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED
				&& context.checkCallingOrSelfPermission(
						android.Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
			return false; // no permission
		}
		return true;
	}

	private boolean askLocationPermission() {
		if (Build.VERSION.SDK_INT < 23)
			return true;
		// TODO add asking code
		return false;
	}

	protected Location getCurrentLocation(LocationManager locManager) {
		if (locManager == null)
			return null;
		Location loc = getBetterLocation(locManager.getLastKnownLocation(LocationManager.GPS_PROVIDER), null);
		loc = getBetterLocation(locManager.getLastKnownLocation(LocationManager.NETWORK_PROVIDER), loc);
		return getBetterLocation(locManager.getLastKnownLocation(LocationManager.PASSIVE_PROVIDER), loc);
	}

	private static final int TWO_MINUTES = 1000 * 60 * 2;

	/**
	 * Determines whether one Location reading is better than the current
	 * Location fix
	 * 
	 * @param location
	 *            The new Location that you want to evaluate
	 * @param currentBestLocation
	 *            The current Location fix, to which you want to compare the new
	 *            one
	 */
	protected Location getBetterLocation(Location location, Location currentBestLocation) {
		if (currentBestLocation == null) {
			// A new location is always better than no location
			return location;
		}
		if (location == null)
			return currentBestLocation;

		// Check whether the new location fix is newer or older
		long timeDelta = location.getTime() - currentBestLocation.getTime();
		boolean isSignificantlyNewer = timeDelta > TWO_MINUTES;
		boolean isSignificantlyOlder = timeDelta < -TWO_MINUTES;
		boolean isNewer = timeDelta > 0;

		// If it's been more than two minutes since the current location, use
		// the new location
		// because the user has likely moved
		if (isSignificantlyNewer) {
			return location;
			// If the new location is more than two minutes older, it must be
			// worse
		} else if (isSignificantlyOlder) {
			return currentBestLocation;
		}

		// Check whether the new location fix is more or less accurate
		int accuracyDelta = (int) (location.getAccuracy() - currentBestLocation.getAccuracy());
		boolean isLessAccurate = accuracyDelta > 0;
		boolean isMoreAccurate = accuracyDelta < 0;
		boolean isSignificantlyLessAccurate = accuracyDelta > 100;  // TODO -> configurable

		// Check if the old and new location are from the same provider
		boolean isFromSameProvider = isSameProvider(location.getProvider(), currentBestLocation.getProvider());

		// Determine location quality using a combination of timeliness and
		// accuracy
		if (isMoreAccurate) {
			return location;
		} else if (isNewer && !isLessAccurate) {
			return location;
		} else if (isNewer && !isSignificantlyLessAccurate && isFromSameProvider) {
			return location;
		}
		return currentBestLocation;
	}

	public Location getLocation() {
		if (!isPermitted()) {
			if (!askLocationPermission())
				return null;
		}
		return getCurrentLocation((LocationManager) context.getSystemService(Context.LOCATION_SERVICE));
	}

	private boolean isSameProvider(String provider1, String provider2) {
		if (provider1 == null) {
			return provider2 == null;
		}
		return provider1.equals(provider2);
	}

}
