import 'package:flutter/material.dart';

class InputField extends StatelessWidget {
  String textHint;
  String errorMsg;
  TextInputType inputType;
  TextEditingController controller = TextEditingController();
  InputField(this.textHint, this.inputType);

  @override
  Widget build(BuildContext context) {
    return Container(
      width: MediaQuery.of(context).size.width * 0.75,
      child: Material(
        color: Colors.grey,
        elevation: 20.0,
        borderRadius: BorderRadius.all(Radius.circular(10.0)),
        child: TextFormField(
          controller: controller,
          keyboardType: this.inputType,
          style: TextStyle(fontSize: 25.0),
          decoration: InputDecoration(
              border: InputBorder.none,
              hintText: this.textHint,
              errorText: errorMsg
          ),
        ),
      ),
    );
  }
}
