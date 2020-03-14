import 'package:auto_size_text/auto_size_text.dart';
import 'package:flutter/material.dart';
import 'package:frontEnd/models/category.dart';
import 'package:frontEnd/utilities/utilities.dart';

class CategoryRow extends StatefulWidget {
  final Category category;
  final VoidCallback onSelect;
  bool selected;
  final String owner;

  CategoryRow(this.category, this.selected, this.owner, {this.onSelect});

  @override
  _CategoryRow createState() => new _CategoryRow();
}

class _CategoryRow extends State<CategoryRow> {
  @override
  Widget build(BuildContext context) {
    return Container(
      height: MediaQuery.of(context).size.height * .07,
      child: Row(
        mainAxisAlignment: MainAxisAlignment.spaceBetween,
        children: <Widget>[
          Checkbox(
            value: widget.selected,
            onChanged: (bool value) {
              this.widget.onSelect();
              setState(() {
                widget.selected = value;
              });
            },
          ),
          Expanded(
            child: AutoSizeText(
              (widget.owner == null)
                  ? widget.category.categoryName
                  : "${widget.category.categoryName} (@${widget.owner})",
              minFontSize: 12,
              maxLines: 1,
              overflow: TextOverflow.ellipsis,
              style: TextStyle(fontSize: 20),
            ),
          ),
        ],
      ),
      decoration: BoxDecoration(
          border: Border(bottom: BorderSide(color: getBorderColor()))),
    );
  }
}
