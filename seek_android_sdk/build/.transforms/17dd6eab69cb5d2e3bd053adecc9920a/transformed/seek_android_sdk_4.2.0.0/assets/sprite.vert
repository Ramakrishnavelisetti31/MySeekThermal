//
// SEEK THERMAL CONFIDENTIAL
// _________________________
//
// Copyright (c) 2019 Seek Thermal Incorporated
// All Rights Reserved.
//
// NOTICE:  All information contained herein is, and remains
// the property of Seek Thermal Incorporated and its suppliers,
// if any.  The intellectual and technical concepts contained
// herein are proprietary to Seek Thermal Incorporated
// and its suppliers and may be covered by U.S. and Foreign Patents,
// patents in process, and are protected by trade secret or copyright law.
// Dissemination of this information or reproduction of this material
// is strictly forbidden unless prior written permission is obtained
// from Seek Thermal Incorporated.
//
precision mediump float;
uniform mat4 u_MVPMatrix;
attribute vec4 a_Position;
attribute vec2 a_TexCoordinate;
varying vec2 v_TexCoordinate;

void main() {
    v_TexCoordinate = a_TexCoordinate;
    gl_Position = u_MVPMatrix * a_Position;
}
