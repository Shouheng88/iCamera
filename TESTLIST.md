# The Test List of iCamera

## 1、Camera1 with TextureView

|No|Item|OnePlus6|
|:-----:|:-----|:-----:|
|1|Open|Pass|
|2|Close|Pass|
|3|Resume|Watch|
|4|Flash|Pass|
||Flash On|Pass|
||Flash Off|Pass|
||Flash Auto|Pass|
|5|Shutter Enable|Pass|
||Shutter Disable|Pass|
|6|Rotation|Pass|
|7|Change size|Watch|
|8|Capture with rear camera (voidce)|Pass|
|9|Capture with front camera (voidce)|Pass|
|10|Record with rear canera (voidce)|Pass|
|11|Recond with front camera (voidce)|Pass|
|12|Memory leak detect|Pass|
|13|Zoom|Pass|
||Zoom with Capture|Pass|
||Zoom with Record|Pass|
|14|Focus|Watch|
|15|Video duration|Pass|

## 2、Camera1 with SurfaceView

|No|Item|OnePlus6|
|:-----:|:-----|:-----:|
|1|Open|Pass|
|2|Close|Pass|
|3|Resume|Watch|
|4|Flash|Pass|
||Flash On|Pass|
||Flash Off|Pass|
||Flash Auto|Pass|
|5|Shutter Enable|Pass|
||Shutter Disable|Pass|
|6|Rotation|Pass|
|7|Change size|Watch|
|8|Capture with rear camera (voidce)|Pass|
|9|Capture with front camera (voidce)|Pass|
|10|Record with rear canera (voidce)|Pass|
|11|Recond with front camera (voidce)|Pass|
|12|Memory leak detect|Pass|
|13|Zoom|Pass|
||Zoom with Capture|Pass|
||Zoom with Record|Pass|
|14|Focus|Watch|
|15|Video duration|Pass|

## 3、Camera2 with TextureView

|No|Item|OnePlus6|
|:-----:|:-----|:-----:|
|1|Open|Pass|
|2|Close|Pass|
|3|Resume|Watch|
|4|Flash|Pass|
||Flash On|Pass|
||Flash Off|Pass|
||Flash Auto|Pass|
|5|Shutter Enable|Pass|
||Shutter Disable|Pass|
|6|Rotation|Pass|
||Rotation with Capture|Pass|
||Rotation with Record|Pass|
|7|Change size|Watch|
|8|Capture with rear camera (voidce)|Pass|
|9|Capture with front camera (voidce)|Pass|
|10|Record with rear canera (voidce)|Pass|
|11|Recond with front camera (voidce)|Pass|
|12|Memory leak detect|Pass|
|13|Zoom|Pass|
||Zoom with Capture|Pass|
||Zoom with Record|Pass|
|14|Focus|Watch|
|15|Video duration|Pass|

Tested on Devices: OnePlus6

## 4、Camera2 with SurfaceView

|No|Item|OnePlus6|
|:-----:|:-----|:-----:|
|1|Open||
|2|Close||
|3|Resume||
|4|Flash||
||Flash On||
||Flash Off||
||Flash Auto||
|5|Shutter Enable||
||Shutter Disable||
|6|Rotation||
||Rotation with Capture||
||Rotation with Record||
|7|Change size||
|8|Capture with rear camera (voidce)||
|9|Capture with front camera (voidce)||
|10|Record with rear canera (voidce)||
|11|Recond with front camera (voidce)||
|12|Memory leak detect||
|13|Zoom||
||Zoom with Capture||
||Zoom with Record||
|14|Focus||
|15|Video duration||

## Issues and Status

### 0.0.1-beta

- [x] Crash when get max zoom while the camera was not prepared (the camera parameters was not calculated when trying to get max zoom)
- [x] sometime failed to calculate video size when switch camera face
- [x] sometime preview is distorted when switch camera face for camera2
