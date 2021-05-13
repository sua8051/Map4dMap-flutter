package vn.map4d.map.map4d_map;

import android.Manifest;
import android.content.Context;
import android.content.pm.PackageManager;
import android.util.Log;
import android.view.View;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;


import io.flutter.plugin.common.BinaryMessenger;
import io.flutter.plugin.common.MethodCall;
import io.flutter.plugin.common.MethodChannel;
import io.flutter.plugin.platform.PlatformView;
import vn.map4d.map.annotations.MFCircle;
import vn.map4d.map.annotations.MFMarker;
import vn.map4d.map.annotations.MFPolygon;
import vn.map4d.map.annotations.MFPolyline;
import vn.map4d.map.camera.MFCameraPosition;
import vn.map4d.map.camera.MFCameraUpdate;
import vn.map4d.map.camera.MFCameraUpdateFactory;
import vn.map4d.map.core.MFCoordinateBounds;
import vn.map4d.map.core.MFMapView;
import vn.map4d.map.core.Map4D;
import vn.map4d.map.core.OnMapReadyCallback;
import vn.map4d.types.MFLocationCoordinate;

public class FMFMapViewController implements
  PlatformView,
  OnMapReadyCallback,
  FMFMapViewOptionsSink,
  MethodChannel.MethodCallHandler,
  FMFMapViewListener {
  private static final String TAG = "FMFMapViewController";
  private final int id;
  private final MethodChannel methodChannel;
  private Map4D map4D;
  @Nullable
  private MFMapView mapView;
  private final Context context;
  private boolean myLocationEnabled = false;
  private boolean myLocationButtonEnabled = false;
  private boolean trackCameraPosition = false;
  private boolean buildingsEnabled = true;
  private boolean rotateGesturesEnabled = true;
  private boolean zoomGesturesEnabled = true;
  private boolean tiltGesturesEnabled = true;
  private boolean scrollGesturesEnabled = true;
  private boolean poisEnabled = true;
  private boolean disposed = false;
  private final float density;

  private Float minZoomPreference = null;
  private Float maxZoomPreference = null;

  private MFCameraPosition initialCameraPosition;

  private final CirclesController circlesController;
  private List<Object> initialCircles;

  FMFMapViewController(@NonNull Context context, int id, BinaryMessenger binaryMessenger, @Nullable MFCameraPosition initialCameraPosition) {
    this.mapView = new MFMapView(context, null);
    this.id = id;
    this.context = context;
    this.density = context.getResources().getDisplayMetrics().density;
    this.methodChannel = new MethodChannel(binaryMessenger, "plugin:map4d-map-view-type_" + id);
    methodChannel.setMethodCallHandler(this);
    this.initialCameraPosition = initialCameraPosition;
    this.circlesController = new CirclesController(methodChannel, density);
  }

  void init() {
    this.mapView.getMapAsync(this);
  }

  @Override
  public View getView() {
    return mapView;
  }

  @Override
  public void dispose() {
    if (disposed) {
      return;
    }
    disposed = true;
    methodChannel.setMethodCallHandler(null);
    setMap4dListener(null);
    destroyMapViewIfNecessary();
  }

  @Override
  public void onMapReady(Map4D map4D) {
    this.map4D = map4D;
    initialMapSettings();
    setMap4dListener(this);
    circlesController.setMap(map4D);
    updateInitialCircles();

    this.map4D.setOnMapModeChange(new Map4D.OnMapModeChangeListener() {
      @Override
      public void onMapModeChange(boolean is3DMode) {
        final Map<String, Object> arguments = new HashMap<>(2);
        arguments.put("is3DMode", is3DMode);
        methodChannel.invokeMethod("map#onModeChange", arguments);
      }
    });
  }

  private void initialMapSettings() {
    if (initialCameraPosition != null) {
      map4D.moveCamera(MFCameraUpdateFactory.newCameraPosition(initialCameraPosition));
    }
    if (minZoomPreference != null) {
      map4D.setMinZoomPreference(minZoomPreference);
    }
    if (maxZoomPreference != null) {
      map4D.setMaxZoomPreference(maxZoomPreference);
    }
    map4D.setBuildingsEnabled(this.buildingsEnabled);
    map4D.setPOIsEnabled(this.poisEnabled);
    map4D.getUiSettings().setZoomGesturesEnabled(zoomGesturesEnabled);
    map4D.getUiSettings().setRotateGesturesEnabled(rotateGesturesEnabled);
    map4D.getUiSettings().setTiltGesturesEnabled(tiltGesturesEnabled);
    map4D.getUiSettings().setScrollGesturesEnabled(scrollGesturesEnabled);
    updateMyLocationSettings();
  }

  private void setMap4dListener(@Nullable FMFMapViewListener listener) {
    map4D.setOnCameraMoveStartedListener(listener);
    map4D.setOnCameraMoveListener(listener);
    map4D.setOnCameraIdleListener(listener);
    map4D.setOnMarkerClickListener(listener);
    map4D.setOnMarkerDragListener(listener);
    map4D.setOnPolygonClickListener(listener);
    map4D.setOnPolylineClickListener(listener);
    map4D.setOnCircleClickListener(listener);
    map4D.setOnMapClickListener(listener);
  }

  @Override
  public void onMethodCall(@NonNull MethodCall call, @NonNull MethodChannel.Result result) {
    switch (call.method) {
      case "camera#move": {
        final MFCameraUpdate cameraUpdate =
          Convert.toCameraUpdate(call.argument("cameraUpdate"), density);
        moveCamera(cameraUpdate);
        result.success(null);
        break;
      }
      case "camera#animate": {
        final MFCameraUpdate cameraUpdate =
          Convert.toCameraUpdate(call.argument("cameraUpdate"), density);
        animateCamera(cameraUpdate);
        result.success(null);
        break;
      }
      case "map#enable3DMode": {
        final boolean isEnable = call.argument("enable3DMode");
        map4D.enable3DMode(isEnable);
        result.success(null);
        break;
      }
      case "map#fitBounds": {
        if (map4D != null) {
          final MFCoordinateBounds bounds = Convert.toCoordinateBounds(call.argument("bounds"));
          final int padding = call.argument("padding");
          final MFCameraPosition cameraPosition = map4D.getCameraPositionForBounds(bounds, padding);
          map4D.moveCamera(MFCameraUpdateFactory.newCameraPosition(cameraPosition));
          result.success(null);
        }
        else {
          result.error(
            "Map4D uninitialized",
            "fitBounds called prior to map initialization",
            null);
        }
        break;
      }
      case "map#cameraForBounds": {
        if (map4D != null) {
          final MFCoordinateBounds bounds = Convert.toCoordinateBounds(call.argument("bounds"));
          final int padding = call.argument("padding");
          MFCameraPosition cameraPosition = map4D.getCameraPositionForBounds(bounds, padding);
          result.success(Convert.cameraPositionToJson(cameraPosition));
        } else {
          result.error(
            "Map4D uninitialized",
            "cameraForBounds called prior to map initialization",
            null);
        }
        break;
      }
      case "map#update":
      {
        Convert.interpretMap4dOptions(call.argument("options"), this);
        result.success(Convert.cameraPositionToJson(getCameraPosition()));
        break;
      }
      case "map#isZoomGesturesEnabled":
      {
        result.success(map4D.getUiSettings().isZoomGesturesEnabled());
        break;
      }
      case "map#isScrollGesturesEnabled":
      {
        result.success(map4D.getUiSettings().isScrollGesturesEnabled());
        break;
      }
      case "map#isTiltGesturesEnabled":
      {
        result.success(map4D.getUiSettings().isTiltGesturesEnabled());
        break;
      }
      case "map#isRotateGesturesEnabled":
      {
        result.success(map4D.getUiSettings().isRotateGesturesEnabled());
        break;
      }
      case "map#isMyLocationButtonEnabled":
      {
        result.success(map4D.getUiSettings().isMyLocationButtonEnabled());
        break;
      }
      case "map#isBuildingsEnabled":
      {
        result.success(map4D.isBuildingsEnabled());
        break;
      }
      case "map#isPOIsEnabled":
      {
        result.success(map4D.isPOIsEnabled());
        break;
      }
      case "map#getZoomLevel": {
        result.success(map4D.getCameraPosition().getZoom());
        break;
      }
      case "circles#update":
      {
        List<Object> circlesToAdd = call.argument("circlesToAdd");
        circlesController.addCircles(circlesToAdd);
        List<Object> circlesToChange = call.argument("circlesToChange");
        circlesController.changeCircles(circlesToChange);
        List<Object> circleIdsToRemove = call.argument("circleIdsToRemove");
        circlesController.removeCircles(circleIdsToRemove);
        result.success(null);
        break;
      }
      default:
        result.notImplemented();
        break;
    }
  }

  private void moveCamera(MFCameraUpdate cameraUpdate) {
    map4D.moveCamera(cameraUpdate);
  }

  private void animateCamera(MFCameraUpdate cameraUpdate) {
    map4D.animateCamera(cameraUpdate);
  }

  private MFCameraPosition getCameraPosition() {
    return trackCameraPosition ? map4D.getCameraPosition() : null;
  }

  private void destroyMapViewIfNecessary() {
    if (mapView == null) {
      return;
    }
    mapView.onDestroy();
    mapView = null;
  }

  private void updateMyLocationSettings() {
    if (hasLocationPermission()) {
      // The plugin doesn't add the location permission by default so that apps that don't need
      // the feature won't require the permission.
      // Gradle is doing a static check for missing permission and in some configurations will
      // fail the build if the permission is missing. The following disables the Gradle lint.
      // noinspection ResourceType
      map4D.setMyLocationEnabled(myLocationEnabled);
      map4D.getUiSettings().setMyLocationButtonEnabled(myLocationButtonEnabled);
    } else {
      Log.e(TAG, "Cannot enable MyLocation layer as location permissions are not granted");
    }
  }

  private boolean hasLocationPermission() {
    return checkSelfPermission(Manifest.permission.ACCESS_FINE_LOCATION)
      == PackageManager.PERMISSION_GRANTED
      || checkSelfPermission(Manifest.permission.ACCESS_COARSE_LOCATION)
      == PackageManager.PERMISSION_GRANTED;
  }

  private int checkSelfPermission(String permission) {
    if (permission == null) {
      throw new IllegalArgumentException("permission is null");
    }
    return context.checkPermission(
      permission, android.os.Process.myPid(), android.os.Process.myUid());
  }

  private void updateInitialCircles() {
    circlesController.addCircles(initialCircles);
  }

  @Override
  public void setMinMaxZoomPreference(Float min, Float max) {
    this.minZoomPreference = min;
    this.maxZoomPreference = max;
    if (map4D == null) {
      return;
    }
    if (min != null) {
      map4D.setMinZoomPreference(min);
    }
    if (max != null) {
      map4D.setMaxZoomPreference(max);
    }
  }

  @Override
  public void setRotateGesturesEnabled(boolean rotateGesturesEnabled) {
    this.rotateGesturesEnabled = rotateGesturesEnabled;
    if (map4D == null) {
      return;
    }
    map4D.getUiSettings().setRotateGesturesEnabled(rotateGesturesEnabled);
  }

  @Override
  public void setScrollGesturesEnabled(boolean scrollGesturesEnabled) {
    this.scrollGesturesEnabled = scrollGesturesEnabled;
    if (map4D == null) {
      return;
    }
    map4D.getUiSettings().setScrollGesturesEnabled(scrollGesturesEnabled);
  }

  @Override
  public void setTiltGesturesEnabled(boolean tiltGesturesEnabled) {
    this.tiltGesturesEnabled = tiltGesturesEnabled;
    if (map4D == null) {
      return;
    }
    map4D.getUiSettings().setTiltGesturesEnabled(tiltGesturesEnabled);
  }

  @Override
  public void setZoomGesturesEnabled(boolean zoomGesturesEnabled) {
    this.zoomGesturesEnabled = zoomGesturesEnabled;
    if (map4D == null) {
      return;
    }
    map4D.getUiSettings().setZoomGesturesEnabled(zoomGesturesEnabled);
  }

  @Override
  public void setTrackCameraPosition(boolean trackCameraPosition) {
    this.trackCameraPosition = trackCameraPosition;
  }

  @Override
  public void setMyLocationEnabled(boolean myLocationEnabled) {
    if (this.myLocationEnabled == myLocationEnabled) {
      return;
    }
    this.myLocationEnabled = myLocationEnabled;
    if (map4D != null) {
      updateMyLocationSettings();
    }
  }

  @Override
  public void setMyLocationButtonEnabled(boolean myLocationButtonEnabled) {
    if (this.myLocationButtonEnabled == myLocationButtonEnabled) {
      return;
    }
    this.myLocationButtonEnabled = myLocationButtonEnabled;
    if (map4D != null) {
      updateMyLocationSettings();
    }
  }

  @Override
  public void setBuildingsEnabled(boolean buildingsEnabled) {
    this.buildingsEnabled = buildingsEnabled;
    if (map4D == null) {
      return;
    }
    map4D.setBuildingsEnabled(buildingsEnabled);
  }

  @Override
  public void setPOIsEnabled(boolean poisEnabled) {
    this.poisEnabled = poisEnabled;
    if (map4D == null) {
      return;
    }
    map4D.setPOIsEnabled(poisEnabled);
  }

  @Override
  public void setInitialCircles(Object initialCircles) {
    ArrayList<?> circles = (ArrayList<?>) initialCircles;
    this.initialCircles = circles != null ? new ArrayList<>(circles) : null;
    if (map4D != null) {
      updateInitialCircles();
    }
  }

  @Override
  public void onCameraIdle() {
    methodChannel.invokeMethod("camera#onIdle", Collections.singletonMap("map", id));
  }

  @Override
  public void onCameraMove() {
    if (!trackCameraPosition) {
      return;
    }
    final Map<String, Object> arguments = new HashMap<>(2);
    arguments.put("position", Convert.cameraPositionToJson(map4D.getCameraPosition()));
    methodChannel.invokeMethod("camera#onMove", arguments);
  }

  @Override
  public void onCameraMoveStarted(int reason) {
    final Map<String, Object> arguments = new HashMap<>(2);
    boolean isGesture = reason == Map4D.OnCameraMoveStartedListener.REASON_GESTURE;
    arguments.put("isGesture", isGesture);
    methodChannel.invokeMethod("camera#onMoveStarted", arguments);
  }

  @Override
  public void onCircleClick(MFCircle mfCircle) {
    circlesController.onCircleTap(mfCircle.getId());
  }

  @Override
  public void onInfoWindowClick(@NonNull MFMarker mfMarker) {

  }

  @Override
  public void onMapClick(MFLocationCoordinate mfLocationCoordinate) {
    final Map<String, Object> arguments = new HashMap<>(2);
    arguments.put("coordinate", Convert.latLngToJson(mfLocationCoordinate));
    methodChannel.invokeMethod("map#didTapAtCoordinate", arguments);
  }

  @Override
  public boolean onMarkerClick(MFMarker mfMarker) {
    return false;
  }

  @Override
  public void onMarkerDrag(MFMarker mfMarker) {

  }

  @Override
  public void onMarkerDragEnd(MFMarker mfMarker) {

  }

  @Override
  public void onMarkerDragStart(MFMarker mfMarker) {

  }

  @Override
  public void onPolygonClick(MFPolygon mfPolygon) {

  }

  @Override
  public void onPolylineClick(MFPolyline mfPolyline) {

  }
}
