"use strict";

myApp.filter("formatDA", function() {
    return function (da) {
        return (da && da.length == 8)
            ? da.substr(0, 4) + "-" + da.substr(4, 2) + "-" + da.substr(6)
            : da;
    };
});

myApp.filter("formatTM", function() {
    return function (tm) {
        if (!tm || tm.length < 3)
            return tm;
        if (tm.charAt(2) === ":")
            return tm.substr(0, 8);
        var hh_mm = tm.substr(0, 2) + ":" + tm.substr(2, 2);
        return tm.length < 5 ? hh_mm : hh_mm + ":" + tm.substr(4, 2);
    }
});

myApp.filter("contentDescription", function() {
    function valueOf(attr) {
        return attr && attr.Value[0];
    };
    function valuesOf(attr) {
        return attr && attr.Value && attr.Value.join();
    };
    function imageDescriptionOf(attrs) {
        var cols = valueOf(attrs["00280011"]); // Columns
        return cols && (cols + "x"
            + valueOf(attrs["00280010"]) + " " // Rows
            + valueOf(attrs["00280100"]) + " bit " // BitsAllocated
            + valuesOf(attrs["00080008"])); // ImageType
    };
    function srDescriptionOf(attrs) {
        var code = valueOf(attrs["0040A043"]); // ConceptNameCodeSequence
        return code && [
                valueOf(attrs["0040A496"]), // PreliminaryFlag
                valueOf(attrs["0040A491"]), // CompletionFlag
                valueOf(attrs["0040A493"]), // VerificationFlag
                valueOf(code["00080104"])  // CodeMeaning
            ].filter(function (obj) { return obj }).join(" ");
    };
    return function(attrs) {
        return valueOf(attrs["00700081"]) // ContentDescription
            || imageDescriptionOf(attrs)
            || srDescriptionOf(attrs)
            || valueOf(inst["00080016"]); // SOPClassUID
    };

});

myApp.filter("formatTag", function() {
    return function (tag) {
        return "(" + tag.slice(0,4) + "," + tag.slice(4, 8) + ")";
    };
});

myApp.filter("formatAttributeValue", function() {
    return function (el) {
        if (el.Value && el.Value.length) {
            switch (el.vr) {
                case 'SQ':
                    return el.Value.length + ' Item(s)';
                case 'PN':
                    return el.Value.map(function(value){
                        return value.Alphabetic;
                    }).join();
                default:
                    return el.Value.join();
            }
        }
        return "";
    };
});

myApp.filter("attributeNameOf", function() {
    return function (tag) {
        return DCM4CHE.elementName.forTag(tag);
    };
});



