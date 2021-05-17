// Copyright 2013 The Flutter Authors. All rights reserved.
// Use of this source code is governed by a BSD-style license that can be
// found in the LICENSE file.

// ignore_for_file: public_member_api_docs

import 'package:flutter/material.dart';
import 'package:map4d_map/map4d_map_flutter.dart'; 
import 'dart:math';
import 'page.dart';

class MarkerIconsPage extends Map4dMapExampleAppPage {
  MarkerIconsPage() : super(const Icon(Icons.room), 'Place marker');

  @override
  Widget build(BuildContext context) {
    return const PlaceMarkerBody();
  }
}

class PlaceMarkerBody extends StatefulWidget {
  const PlaceMarkerBody();

  @override
  State<StatefulWidget> createState() => PlaceMarkerBodyState();
}

class PlaceMarkerBodyState extends State<PlaceMarkerBody> {
  PlaceMarkerBodyState() {
    final MFMarkerId markerId = MFMarkerId('marker_id_0');
    final MFMarker marker = MFMarker(
        consumeTapEvents: true,
        markerId: markerId,
        position: LatLng(16.0324816, 108.132791),
        anchor: Offset(0.5, 1.0),
        infoWindow: InfoWindow(
            snippet: "Snippet",
            title: "Map4D",
            anchor: const Offset(0.5, 0.0),
            onTap: () {
              _onInfoWindowTapped(markerId);
            }),
        zIndex: 1.0,
        onTap: () {
          _onMarkerTapped(markerId);
        },
        onDragEnd: (LatLng position) {
          _onMarkerDragEnd(markerId, position);
        });
    markers[markerId] = marker;
  }

  MFMapViewController? controller;
  Map<MFMarkerId, MFMarker> markers = <MFMarkerId, MFMarker>{};
  int _markerIdCounter = 1;
  int _indexPosition = 0;
  MFMarkerId? selectedMarker;
  int currentZIndex = 0;
  List<double> zIndexs = <double>[
    0.0,
    10.0,
  ];

  int currentRotation = 0;
  List<double> rotations = <double>[
    0.0,
    30.0,
    60.0,
    90.0,
    120.0,
    150.0,
    180.0,
    210.0,
    240.0,
    270.0,
    300.0,
    330.0,
    360.0
  ];

  int currentElevation = 0;
  List<double> elevations = <double>[0.0, 100.0, 200.0, 1000.0];

  // Values when toggling circle stroke width
  int widthsIndex = 0;
  List<int> widths = <int>[10, 20, 5];

  void _onMapCreated(MFMapViewController controller) {
    this.controller = controller;
  }

  @override
  void dispose() {
    super.dispose();
  }

  void _onInfoWindowTapped(MFMarkerId markerId) {
    setState(() {
      print("Did tap info window of " + markerId.value);
    });
  }

  void _onMarkerTapped(MFMarkerId markerId) {
    setState(() {
      selectedMarker = markerId;
    });
  }

  void _onMarkerDragEnd(MFMarkerId markerId, LatLng newPosition) async {
    final MFMarker? tappedMarker = markers[markerId];
    if (tappedMarker != null) {
      await showDialog<void>(
          context: context,
          builder: (BuildContext context) {
            return AlertDialog(
                actions: <Widget>[
                  TextButton(
                    child: const Text('OK'),
                    onPressed: () => Navigator.of(context).pop(),
                  )
                ],
                content: Padding(
                    padding: const EdgeInsets.symmetric(vertical: 66),
                    child: Column(
                      mainAxisSize: MainAxisSize.min,
                      children: <Widget>[
                        Text('Old position: ${tappedMarker.position}'),
                        Text('New position: $newPosition'),
                      ],
                    )));
          });
    }
    final MFMarker marker = markers[markerId]!;
    setState(() {
      markers[markerId] = marker.copyWith(
        positionParam: newPosition
      );
    });
  }

  void _remove(MFMarkerId markerId) {
    setState(() {
      if (markers.containsKey(markerId)) {
        markers.remove(markerId);
      }
      if (markerId == selectedMarker) {
        selectedMarker = null;
      }
    });
  }

  void _add() {
    final int markerCount = markers.length;

    if (markerCount == 13) {
      return;
    }

    final String markerIdVal = 'marker_id_$_markerIdCounter';
    _markerIdCounter++;
    final MFMarkerId markerId = MFMarkerId(markerIdVal);

    final MFMarker marker = MFMarker(
      consumeTapEvents: true,
      markerId: markerId,
      position: _createCenter(),
      infoWindow: InfoWindow(
          snippet: "Snippet",
          title: "Map4D",
          anchor: const Offset(0.5, 0.0),
          onTap: () {
            _onInfoWindowTapped(markerId);
          }),
      zIndex: 1.0,
      onTap: () {
        _onMarkerTapped(markerId);
      },
      onDragEnd: (LatLng position) {
        _onMarkerDragEnd(markerId, position);
      },
    );
    setState(() {
      markers[markerId] = marker;
    });
  }

  void _changePostion(MFMarkerId markerId) {
    if (_indexPosition >= 8) {
      _indexPosition = 0;
    }

    final MFMarker marker = markers[markerId]!;
    setState(() {
      markers[markerId] = marker.copyWith(
        positionParam: LatLng(
            16.0324816 + sin(_indexPosition * pi / 4.0) / 6.0 * 0.8,
            108.132791 + cos(_indexPosition * pi / 4.0) / 6.0),
      );
      _indexPosition += 1;
    });
  }

  Future<void> _changeInfo(MFMarkerId markerId) async {
    final MFMarker marker = markers[markerId]!;
    final String newSnippet = marker.infoWindow.snippet! + '*****';
    setState(() {
      markers[markerId] = marker.copyWith(
        infoWindowParam: marker.infoWindow.copyWith(
          snippetParam: newSnippet,
        ),
      );
    });
  }

  void _changeRotation(MFMarkerId markerId) {
    final MFMarker marker = markers[markerId]!;
    setState(() {
      markers[markerId] = marker.copyWith(
        rotationParam: rotations[++currentRotation % rotations.length],
      );
    });
  }

  void _changeElevation(MFMarkerId markerId) {
    final MFMarker marker = markers[markerId]!;
    setState(() {
      markers[markerId] = marker.copyWith(
        elevationParam: elevations[++currentElevation % elevations.length],
      );
    });
  }

  void _changeDraggable(MFMarkerId markerId) {
    final MFMarker marker = markers[markerId]!;
    setState(() {
      markers[markerId] = marker.copyWith(
        draggableParam: !marker.draggable,
      );
    });
  }

  void _changeVisible(MFMarkerId markerId) {
    final MFMarker marker = markers[markerId]!;
    setState(() {
      markers[markerId] = marker.copyWith(
        visibleParam: !marker.visible,
      );
    });
  }

  void _changeZindex(MFMarkerId markerId) {
    final MFMarker marker = markers[markerId]!;
    setState(() {
      markers[markerId] = marker.copyWith(
        zIndexParam: zIndexs[++currentZIndex % zIndexs.length],
      );
    });
  }

  @override
  Widget build(BuildContext context) {
    final MFMarkerId? selectedId = selectedMarker;
    return Column(
      mainAxisAlignment: MainAxisAlignment.spaceEvenly,
      crossAxisAlignment: CrossAxisAlignment.stretch,
      children: <Widget>[
        Center(
          child: SizedBox(
            width: 350.0,
            height: 300.0,
            child: MFMapView(
              initialCameraPosition: const MFCameraPosition(
                target: LatLng(16.0324816, 108.132791),
                zoom: 10.0,
              ),
              markers: Set<MFMarker>.of(markers.values),
              onMapCreated: _onMapCreated,
            ),
          ),
        ),
        Expanded(
          child: SingleChildScrollView(
            child: Row(
              mainAxisAlignment: MainAxisAlignment.spaceEvenly,
              children: <Widget>[
                Row(
                  children: <Widget>[
                    Column(
                      children: <Widget>[
                        TextButton(
                          child: const Text('add'),
                          onPressed: _add,
                        ),
                        TextButton(
                          child: const Text('remove'),
                          onPressed: (selectedId == null)
                              ? null
                              : () => _remove(selectedId),
                        ),
                        TextButton(
                            child: const Text('change info'),
                            onPressed: (selectedId == null)
                                ? null
                                : () => _changeInfo(selectedId)),
                      ],
                    ),
                    Column(
                      children: <Widget>[
                        TextButton(
                          child: const Text('change position'),
                          onPressed: (selectedId == null)
                              ? null
                              : () => _changePostion(selectedId),
                        ),
                        TextButton(
                          child: const Text('change elevation'),
                          onPressed: (selectedId == null)
                              ? null
                              : () => _changeElevation(selectedId),
                        ),
                        TextButton(
                          child: const Text('change rotation'),
                          onPressed: (selectedId == null)
                              ? null
                              : () => _changeRotation(selectedId),
                        ),
                        TextButton(
                          child: const Text('change draggable'),
                          onPressed: (selectedId == null)
                              ? null
                              : () => _changeDraggable(selectedId),
                        ),
                        TextButton(
                          child: const Text('change visible'),
                          onPressed: (selectedId == null)
                              ? null
                              : () => _changeVisible(selectedId),
                        ),
                        TextButton(
                          child: const Text('change zIndex'),
                          onPressed: (selectedId == null)
                              ? null
                              : () => _changeZindex(selectedId),
                        ),
                      ],
                    )
                  ],
                )
              ],
            ),
          ),
        ),
      ],
    );
  }

  LatLng _createCenter() {
    return _createLatLng(
        16.0324816 + sin(_markerIdCounter * pi / 6.0) / 10.0 * 0.8,
        108.132791 + cos(_markerIdCounter * pi / 6.0) / 10.0);
  }

  LatLng _createLatLng(double lat, double lng) {
    return LatLng(lat, lng);
  }
}
