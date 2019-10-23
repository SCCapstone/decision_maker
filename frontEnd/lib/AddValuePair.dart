import 'package:flutter/material.dart';
import 'package:http/http.dart' as http;

class AddValuePair extends StatefulWidget {
  @override
  _StartScreenState createState() => _StartScreenState();
}

class _StartScreenState extends State<AddValuePair> {
  final String addPairApiEndpoint = "https://9zh1udqup3.execute-api.us-east-2.amazonaws.com/beta";

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
                  //border: InputBorder.none,
                ),
              ),
              TextFormField(
                controller: valueController,
                decoration: InputDecoration(
                  labelText: "Enter a value",
                  //border: InputBorder.none,
                ),
              ),
              RaisedButton.icon(
                  onPressed: () {
                    processAddPair(keyController.text, valueController.text);
                  },
                  icon: Icon(Icons.add),
                  label: Text("Add Pair"))
            ],
          ),
        ),
      ),
    );
  }

  void processAddPair(String key, String value) async {
    http.Response response = await http.post(
        this.addPairApiEndpoint,
        body: "{\"" + key + "\": \"" + value + "\"}");

    if (response.statusCode == 200 && response.body == "Data inserted successfully!") {
      this._showDialog("Data inserted successfully!");
    } else {
      this._showDialog("Unable to insert pair, ensure the key does not start with a number and does not contain spaces.");
    }
  }

  void _showDialog(String message) {
    showDialog(
        context: context,
        builder: (context) {
          return AlertDialog(
            title: Text("Response status:"),
            content: Text(message),
          );
        }
    );
  }
}