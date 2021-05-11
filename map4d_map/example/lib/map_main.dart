import 'package:flutter/cupertino.dart';
import 'package:flutter/material.dart';
import 'dart:async';

import 'package:flutter/services.dart';
import 'package:map4d_map/map4d_map_flutter.dart';
import 'page.dart';

class Map4dApp extends Map4dMapExampleAppPage {
  Map4dApp() : super(const Icon(Icons.map), 'Map4dMap SDK');

  @override
  Widget build(BuildContext context) {
    return const MyApp();
  }
}

class MyApp extends StatefulWidget {
  @override
  _MyAppState createState() => _MyAppState();

  const MyApp();
}

class _MyAppState extends State<MyApp> {
  bool _isMapCreated = false;
  late MFMapViewController _controller;

  @override
  void initState() {
    super.initState();
  }

  @override
  Widget build(BuildContext context) {

    final position = MFCameraPosition(target: LatLng(21.030041, 105.8502223), zoom: 16);
    final MFMapView map = MFMapView(
      initialCameraPosition: position,
      onMapCreated: onMapCreated,
      onCameraMoveStarted: onCameraMoveStarted,
      onCameraMove: onCameraMove,
      onCameraIdle: onCameraIdle,
      myLocationEnabled: true,
      myLocationButtonEnabled: true,
    );

    return MaterialApp(
      home: Scaffold(
        body: Stack(
          children: <Widget>[
            SizedBox(
              width: 480.0,
              height: 800.0,
              child: map,
            ),
            Padding(
              padding: const EdgeInsets.all(1.0),
              child: Align(
                alignment: Alignment.topLeft,
                child: TextButton(
                  child: Text('Move Camera', style: TextStyle(color: Colors.black)),
                  onPressed: moveCamera,
                ),
              ),
            ),
            Padding(
              padding: const EdgeInsets.all(1.0),
              child: Align(
                alignment: Alignment.topCenter,
                child: TextButton(
                  child: Text('Get ZoomLevel', style: TextStyle(color: Colors.black)),
                  onPressed: getZoomLevel,
                ),
              ),
            ),
            Padding(
              padding: const EdgeInsets.all(1.0),
              child: Align(
                alignment: Alignment.topRight,
                child: TextButton(
                  child: Text('Animate Camera', style: TextStyle(color: Colors.black)),
                  onPressed: animateCamera,
                ),
              ),
            )
          ],
        )
      ),
    );
  }

  void onMapCreated(MFMapViewController controller) {
    print('onMapCreated');
    setState(() {
      _controller = controller;
      _isMapCreated = true;
    });
  }

  void onCameraMoveStarted() {
    print('camera move startedddddddddddddddddd');
  }

  void onCameraMove(MFCameraPosition position) {
    print('camera move to ${position.toString()}');
  }

  void onCameraIdle() => print('onCameraIdle');

  void moveCamera() {
    final cameraUpdate = MFCameraUpdate.newLatLng(LatLng(10.779348472547028, 106.71295166015625));
    print('moveCamera to: ' + cameraUpdate.toString());
    _controller.moveCamera(cameraUpdate);
  }

  void animateCamera() {
    final cameraUpdate = MFCameraUpdate.newLatLng(LatLng(16.088414, 108.230563));
    print('animateCamera to: ' + cameraUpdate.toString());
    _controller.animateCamera(cameraUpdate);
  }


  void getZoomLevel() {
    print("Get Zoom level");
    final zoom = _controller.getZoomLevel();
    zoom.then((value) => print('zoom level: ' + value.toString()));
  }
}
