import 'package:flutter/material.dart';

import 'includes/dev_testing_manager.dart';

class AddValuePair extends StatefulWidget {
  @override
  _StartScreenState createState() => _StartScreenState();
}

class _StartScreenState extends State<AddValuePair> {

  final keyController = TextEditingController();
  final valueController = TextEditingController();

  @override
  void dispose() {
    keyController.dispose();
    valueController.dispose();
    super.dispose();
  }

  @override
  Widget build(BuildContext context) {
    return Scaffold(
      appBar: AppBar(
        title: Text("Add New Value Pair"),
      ),
      body: Center(
        child: Padding(
          padding: EdgeInsets.all(20.0),
          child: Column(
            children: <Widget>[
              TextFormField(
                controller: keyController,
                decoration: InputDecoration(
                  labelText: "Enter a key",
                ),
              ),
              TextFormField(
                controller: valueController,
                decoration: InputDecoration(
                  labelText: "Enter a value",
                ),
              ),
              RaisedButton.icon(
                  onPressed: () {
                    DevTestingManager.processAddPair(keyController.text, valueController.text, context);
                  },
                  icon: Icon(Icons.add),
                  label: Text("Add Pair"))
            ],
          ),
        ),
      ),
    );
  }
}