// Copyright 2017 The Chromium Authors. All rights reserved.
// Use of this source code is governed by a BSD-style license that can be
// found in the LICENSE file.

import 'dart:async';
import 'dart:io';
import 'dart:convert' show utf8, json;

import 'package:flutter/material.dart';
import 'package:flutter/services.dart';
import 'package:flutter_secure_storage/flutter_secure_storage.dart';

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
    retrieveUuid();
    eventChannel.receiveBroadcastStream().listen(_onEvent, onError: _onError);
  }

  void _onEvent(Object event) {
    var e = event as Map<String, String>;
    setState(() {
      var msg = '[${e["action"]}] ${e["namespace"].substring(
          0, 4)}:${e["beacon_id"].substring(0, 4)} @${e["rssi"]}';

      event_list.insert(0, msg);
    });

    e["user_id"] = _userUuid;
    _uploadbeacondata(json.encode(event).toString());
  }

  void _onError(PlatformException error) {
    setState(() {
      _chargingStatus = 'Battery status: unknown.';
    });
  }

  final GlobalKey<ScaffoldState> _scaffoldKey = new GlobalKey<ScaffoldState>();
  
  @override
  Widget build(BuildContext context) {
    return new DefaultTabController(length: 3,
        child: new Scaffold(
          key: _scaffoldKey,
            appBar: new AppBar(
              bottom: new TabBar(tabs: [
                new Tab(icon: new Icon(Icons.account_balance_wallet)),
                new Tab(icon: new Icon(Icons.add_shopping_cart)),
                new Tab(icon: new Icon(Icons.add_a_photo)),
              ],
              ),
              title: new Text('Tabs Demo'),
            ),
            body: new TabBarView(children: [
              new BeaconStatus(),
              configPage(),
              new Icon(Icons.add_shopping_cart),
            ])
        ));
  }


  final GlobalKey<FormState> _formKey = new GlobalKey<FormState>();
  String _userUuid;

  bool _autoValidate = false;
  
  void _handleSubmitted() {
    final form = _formKey.currentState;
    if (!form.validate()) {
      _autoValidate = true;
      showInSnackBar("Please fix the errors in red before submitting.");
    } else {
      form.save();
      storeUuid(_userUuid);

      showInSnackBar("Saved ${_userUuid}");
    }
  }

  void showInSnackBar(String value) {
    _scaffoldKey.currentState.showSnackBar(new SnackBar(
        content: new Text(value)
    ));
  }

  final GlobalKey<FormFieldState<String>> _uuidFieldKey = new GlobalKey<FormFieldState<String>>();
  String _validateUUID(String value) {
    final uuidField = _uuidFieldKey.currentState;
    if (uuidField.value == null || uuidField.value.isEmpty)
      return "Please enter a User UUID";
    else
      return null;
  }

  Widget configPage() {
    return new Form(
      key: _formKey,
        child: new ListView(
            children: <Widget>[
              new TextFormField(
                key: _uuidFieldKey,
                decoration: const InputDecoration(
                  labelText: "User UUID",
                  hintText: "The UUID created during registration",
                ),
                validator: _validateUUID,
                onSaved: (val) => _userUuid = val,
              ),
              new RaisedButton(
                child: const Text('SUBMIT'),
                onPressed: _handleSubmitted,
              ),           ]
                
        )
    );
    
  }

  void _uploadbeacondata(Object event) async {
    var httpClient = new HttpClient();
    HttpClientRequest request = await httpClient.postUrl(
        Uri.parse("$ENDPOINT/api/v1/beacon"))
      ..headers.contentType = ContentType.JSON;
    request.write(event.toString());
    HttpClientResponse response = await request.close();
    if (response.statusCode != HttpStatus.OK) {
      event_list.insert(0, 'Failed to send beacon data to backend');
    }
  }

  void storeUuid(String userUuid) async {
    var storage = new FlutterSecureStorage();
    await storage.write(key: "userUuid", value: _userUuid);
  }

  void retrieveUuid() async {
    var storage = new FlutterSecureStorage();
    var localUuid = await storage.read(key: "userUuid");
    if (localUuid == null) {
      localUuid = "6d318d24-6d8e-45c2-8e04-b1cb093a60e6";
    }
    _userUuid = localUuid;
  }
}

class BeaconStatus extends StatefulWidget {
  @override
  createState() => new beaconState();
}

class beaconState extends State<BeaconStatus> {
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
      itemBuilder: (context, index) {
        if (index > event_list.length - 1)
          return null;
        return new Text(event_list[index]);
      },
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
