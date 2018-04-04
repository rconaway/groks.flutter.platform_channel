// Copyright 2017 The Chromium Authors. All rights reserved.
// Use of this source code is governed by a BSD-style license that can be
// found in the LICENSE file.

import 'dart:async';
import 'dart:io';

import 'package:flutter/material.dart';
import 'package:flutter/services.dart';

const ENDPOINT = "http://ec2-18-216-197-13.us-east-2.compute.amazonaws.com";

class PlatformChannel extends StatefulWidget {
  @override
  _PlatformChannelState createState() => new _PlatformChannelState();
}

var event_list = <String>[];

class _PlatformChannelState extends State<PlatformChannel> {
  static const MethodChannel methodChannel =
      const MethodChannel('samples.flutter.io/battery');
  static const EventChannel eventChannel =
      const EventChannel('samples.flutter.io/charging');

  String _batteryLevel = 'Battery level: unknown.';
  String _chargingStatus = 'Battery status: unknown.';

  Future<Null> _getBatteryLevel() async {
    String batteryLevel;
    try {
      final int result = await methodChannel.invokeMethod('getBatteryLevel');
      batteryLevel = 'Battery level: $result%.';
    } on PlatformException {
      batteryLevel = 'Failed to get battery level.';
    }
    setState(() {
      _batteryLevel = batteryLevel;
    });
  }

  @override
  void initState() {
    super.initState();

    eventChannel.receiveBroadcastStream().listen(_onEvent, onError: _onError);
  }

  void _onEvent(Object event) {
    setState(() {
      event_list.insert(0, event);
    });
    _uploadbeacondata(event);
  }

  void _onError(PlatformException error) {
    setState(() {
      _chargingStatus = 'Battery status: unknown.';
    });
  }

  @override
  Widget build(BuildContext context) {
    return new beaconStatus();
  }

  void _uploadbeacondata(Object event) async {
    var httpClient = new HttpClient();
    HttpClientRequest request = await httpClient.postUrl(Uri.parse("$ENDPOINT/api/v1/beacon"))
    ..headers.contentType = ContentType.JSON;
    request.write(event.toString());
    HttpClientResponse response = await request.close();
    if (response.statusCode != HttpStatus.OK) {
      event_list.insert(0, 'Failed to send beacon data to backend');
    }
  }
}

class beaconStatus extends StatefulWidget {
  @override
  createState() => new beaconState();
}

class beaconState extends State<beaconStatus> {
  final _biggerFont = const TextStyle(fontSize: 18.0);

  ScrollController _scrollController;

  @override
  void initState() {
    super.initState();
    _scrollController = new ScrollController(
        initialScrollOffset: 0.0,
        keepScrollOffset: true
    );
  }

  Widget _buildEvents() {
    return new ListView.builder(
        controller: _scrollController,
        padding: const EdgeInsets.all(16.0),
        reverse: true,
        itemBuilder: (context, i) {
          if (i.isOdd) return new Divider();
          final index = i ~/ 2;
          if (index < event_list.length) return _buildRow(event_list[index]);
          else return _buildRow('');
        },
    );
  }

  Widget _buildRow(String text) {
    return new ListTile(
      title: new Text(
        text,
        style: _biggerFont,
      ),
    );
  }

  @override
  Widget build(BuildContext context) {
    return new Scaffold(
      appBar: new AppBar(
        title: new Text('List of events'),
      ),
      body: _buildEvents(),
      );
  }

}

void main() {
  runApp(new MaterialApp(home: new PlatformChannel()));
}
