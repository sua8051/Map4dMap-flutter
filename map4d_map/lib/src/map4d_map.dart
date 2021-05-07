part of map4d_map_flutter;

/// Pass to [Map4dMap.onMapCreated] to receive a [MFMapViewController] when the map is created.
typedef void MapCreatedCallback(MFMapViewController controller);

class MFMapView extends StatefulWidget {

  const MFMapView({
    Key? key,
    this.onMapCreated
  })  : super(key: key);

  @override
  State createState() => _MFMapViewState();

  /// Callback method for when the map is ready to be used.
  /// Used to receive a [MFMapViewController] for this [Map4dMap].
  final MapCreatedCallback? onMapCreated;
}

class _MFMapViewState extends State<MFMapView> {

  // Hmm, it's not pass to native or get from call back
  // final _mapId = _nextMapCreationId++;
  final Completer<MFMapViewController> _controller = Completer<MFMapViewController>();

  @override
  Widget build(BuildContext context) {    
    // This is used in the platform side to register the view.
    final String viewType = 'plugin:map4d-map-view-type';
    // Pass parameters to the platform side.
    final Map<String, dynamic> creationParams = <String, dynamic>{};

    if (defaultTargetPlatform == TargetPlatform.android) {
      return AndroidView(
      viewType: viewType,
      onPlatformViewCreated: onPlatformViewCreated,
      creationParams: creationParams,
      creationParamsCodec: const StandardMessageCodec(),
    );
    } else if (defaultTargetPlatform == TargetPlatform.iOS) {
      return UiKitView(
        viewType: viewType,
        onPlatformViewCreated: onPlatformViewCreated,
        creationParams: creationParams,
        creationParamsCodec: const StandardMessageCodec(),
      );
    }
    return Text('$defaultTargetPlatform is not yet supported by the maps plugin');    
  }

  Future<void> onPlatformViewCreated(int id) async {
    final controller = await MFMapViewController.init(id, this);
    _controller.complete(controller);
    final MapCreatedCallback? onMapCreated = widget.onMapCreated;
    if (onMapCreated != null) {
      onMapCreated(controller);
    }
  }
}