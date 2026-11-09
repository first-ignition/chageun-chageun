#!/usr/bin/env python3
"""
차 흉내(에코): 차 화면의 공조 버튼을 누르면 실제 상태가 바뀌게 해 준다.

왜 필요한가
  실차에서는 하단 바의 제상 버튼을 누르면 → 공조 장치가 켜지고 → 상태가 "켜짐"으로 바뀐다.
  에뮬레이터는 버튼 요청(VHAL set_value)을 기록만 하고, 실제 상태(actual)는 Virtual Console만 바꾼다.
  그래서 버튼을 눌러도 앱이 받는 상태 값은 그대로다.
  이 스크립트는 그 요청을 logcat에서 읽어 Virtual Console로 같은 값을 넣어, 실차처럼 "누르면 바뀌게" 한다.
  앱 코드는 건드리지 않는다. 시연·영상 녹화 때만 켠다.

사용: python3 tools/car-echo.py      (Ctrl+C로 끝)
"""
import os
import re
import subprocess
import sys

sys.path.insert(0, os.path.dirname(os.path.abspath(__file__)))
import vc  # noqa: E402

# AAOS VehiclePropertyIds
HVAC_FAN_SPEED = 356517120
HVAC_TEMPERATURE_SET = 358614275
HVAC_DEFROSTER = 320865540
HVAC_AC_ON = 354419973
HVAC_RECIRC_ON = 354419976
HVAC_AUTO_ON = 354419978
HVAC_SEAT_TEMPERATURE = 356517131
HVAC_STEERING_WHEEL_HEAT = 289408269

# 좌석 영역 비트(VehicleAreaSeat): ROW_1_LEFT=0x1, ROW_1_RIGHT=0x4 / 유리 영역(VehicleAreaWindow): FRONT=0x1, REAR=0x2
SEAT_LEFT, SEAT_RIGHT = 0x1, 0x4
GLASS_FRONT, GLASS_REAR = 0x1, 0x2

LINE = re.compile(r"vhal set_value: VehiclePropValue \{.*?prop: (\d+), area_id: (\d+).*?int32_values: \[([^\]]*)\].*?float_values: \[([^\]]*)\]")


def translate(prop, area, ints, floats):
	"""(prop, area, 값) → [(기능 id, 영역, 값), ...]"""
	i = ints[0] if ints else None
	f = floats[0] if floats else None
	out = []
	if prop == HVAC_AUTO_ON and i is not None:
		out.append(("hvac-auto", None, bool(i)))
	elif prop == HVAC_AC_ON and i is not None:
		out.append(("hvac-ac", None, bool(i)))
	elif prop == HVAC_RECIRC_ON and i is not None:
		out.append(("hvac-recirc", None, bool(i)))
	elif prop == HVAC_FAN_SPEED and i is not None:
		out.append(("hvac-fan-speed", None, i))
	elif prop == HVAC_DEFROSTER and i is not None:
		if area & GLASS_FRONT:
			out.append(("hvac-defroster", "front_defroster_on", bool(i)))
		if area & GLASS_REAR:
			out.append(("hvac-defroster", "rear_defroster_on", bool(i)))
	elif prop == HVAC_TEMPERATURE_SET and f is not None:
		if area & SEAT_LEFT:
			out.append(("hvac-temperature-set", "driver", round(f, 1)))
		if area & SEAT_RIGHT:
			out.append(("hvac-temperature-set", "passenger", round(f, 1)))
	elif prop == HVAC_SEAT_TEMPERATURE and i is not None:
		if area & SEAT_LEFT:
			out.append(("hvac-seat-temperature", "driver", i))
		if area & SEAT_RIGHT:
			out.append(("hvac-seat-temperature", "passenger", i))
	elif prop == HVAC_STEERING_WHEEL_HEAT and i is not None:
		out.append(("steering-wheel-heat", None, i))
	return out


def nums(s, conv):
	return [conv(x) for x in s.split(",") if x.strip()]


def main():
	vc.ensure_forward()
	vc.start_heartbeat()
	cat = vc.catalog()
	print("차 흉내 시작: 차 화면의 공조 버튼을 누르면 실제 상태로 반영합니다. (Ctrl+C로 끝)", flush=True)
	proc = subprocess.Popen(["adb", "logcat", "-T", "1", "-s", "sdv_vhal_rust:I"], stdout=subprocess.PIPE, text=True)
	try:
		for line in proc.stdout:
			m = LINE.search(line)
			if not m:
				continue
			prop, area = int(m.group(1)), int(m.group(2))
			acts = translate(prop, area, nums(m.group(3), int), nums(m.group(4), float))
			for fid, a, v in acts:
				vc.set_value(cat, fid, v, a)
	except KeyboardInterrupt:
		pass
	finally:
		proc.terminate()


if __name__ == "__main__":
	main()
