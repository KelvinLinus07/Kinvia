#!/usr/bin/env python3
"""
Converts the transport workbook into the CSV files that Kinvia imports on start-up.

    python tools/build_timetable_csv.py [path/to/workbook.xlsx]

Needs openpyxl (pip install openpyxl). The generated CSVs are committed, so you only need to run this
when the workbook changes. Everything in train_services.csv / bus_services.csv is copied from the
workbook as-is. The workbook lists station *names* only, so stations.csv adds the fields Kinvia's
Station table requires (code, city, state, approximate coordinates) from the reference table below.
"""
import csv
import re
import sys
from pathlib import Path

from openpyxl import load_workbook

ROOT = Path(__file__).resolve().parent.parent
DEFAULT_WORKBOOK = ROOT / "data" / "source" / "transport_routes_final_separate_ac_2ac_3ac.xlsx"
OUT_DIR = ROOT / "src" / "main" / "resources" / "data"

# name -> (city, state, latitude, longitude). Coordinates are approximate town-centre values; they are only
# used to estimate distance and travel time, because the workbook has no arrival times or distances.
WB, BR, JH = "West Bengal", "Bihar", "Jharkhand"
STATION_REFERENCE = {
    "Howrah Junction": ("Kolkata", WB, 22.5839, 88.3425),
    "Kolkata Sealdah": ("Kolkata", WB, 22.5697, 88.3697),
    "Asansol Junction": ("Asansol", WB, 23.6850, 86.9740),
    "Kharagpur Junction": ("Kharagpur", WB, 22.3396, 87.3200),
    "New Jalpaiguri Junction": ("Siliguri", WB, 26.6950, 88.4102),
    "Durgapur": ("Durgapur", WB, 23.4877, 87.3197),
    "Malda Town": ("Malda", WB, 25.0108, 88.1411),
    "Burdwan Junction": ("Bardhaman", WB, 23.2513, 87.8555),
    "Santragachi Junction": ("Howrah", WB, 22.5838, 88.2822),
    "New Cooch Behar": ("Cooch Behar", WB, 26.3300, 89.4700),
    "Rampurhat Junction": ("Rampurhat", WB, 24.1741, 87.7803),
    "Bandel Junction": ("Bandel", WB, 22.9310, 88.3814),
    "Kolkata Railway Station": ("Kolkata", WB, 22.6172, 88.3789),
    "Adra Junction": ("Adra", WB, 23.4980, 86.6800),
    "Bolpur Shantiniketan": ("Bolpur", WB, 23.6800, 87.7100),
    "Bankura Junction": ("Bankura", WB, 23.2324, 87.0650),
    "Katwa Junction": ("Katwa", WB, 23.6500, 88.1300),
    "Hijli": ("Kharagpur", WB, 22.3200, 87.3100),
    "Purulia Junction": ("Purulia", WB, 23.3320, 86.3650),
    "Dankuni Junction": ("Dankuni", WB, 22.6800, 88.3000),
    "Chittaranjan": ("Chittaranjan", WB, 23.8700, 86.8500),
    "Siliguri Junction": ("Siliguri", WB, 26.7271, 88.3953),
    "Ranaghat Junction": ("Ranaghat", WB, 23.1800, 88.5800),
    "Andal Junction": ("Andal", WB, 23.5900, 87.2400),
    "Krishnanagar City Junction": ("Krishnanagar", WB, 23.4100, 88.4900),
    "Digha": ("Digha", WB, 21.6300, 87.5500),
    "Shalimar": ("Howrah", WB, 22.5600, 88.3100),
    "Midnapore": ("Midnapore", WB, 22.4300, 87.3200),
    "Berhampore Court": ("Berhampore", WB, 24.1000, 88.2500),
    "Naihati Junction": ("Naihati", WB, 22.8900, 88.4200),
    "Patna Junction": ("Patna", BR, 25.6093, 85.1376),
    "Danapur": ("Patna", BR, 25.6300, 85.0400),
    "Muzaffarpur Junction": ("Muzaffarpur", BR, 26.1200, 85.3900),
    "Gaya Junction": ("Gaya", BR, 24.8000, 85.0000),
    "Bhagalpur Junction": ("Bhagalpur", BR, 25.2500, 87.0100),
    "Darbhanga Junction": ("Darbhanga", BR, 26.1600, 85.9000),
    "Samastipur Junction": ("Samastipur", BR, 25.8600, 85.7800),
    "Barauni Junction": ("Barauni", BR, 25.4700, 85.9700),
    "Katihar Junction": ("Katihar", BR, 25.5400, 87.5700),
    "Chhapra Junction": ("Chhapra", BR, 25.7800, 84.7300),
    "Ara Junction": ("Arrah", BR, 25.5600, 84.6600),
    "Buxar Junction": ("Buxar", BR, 25.5700, 83.9800),
    "Hajipur Junction": ("Hajipur", BR, 25.6900, 85.2100),
    "Sonpur Junction": ("Sonpur", BR, 25.7000, 85.1800),
    "Saharsa Junction": ("Saharsa", BR, 25.8800, 86.6000),
    "Purnea Junction": ("Purnia", BR, 25.7800, 87.4700),
    "Kishanganj": ("Kishanganj", BR, 26.1000, 87.9500),
    "Dehri-on-Sone": ("Dehri", BR, 24.9100, 84.1800),
    "Sasaram Junction": ("Sasaram", BR, 24.9500, 84.0300),
    "Motihari (Bapudham Motihari)": ("Motihari", BR, 26.6500, 84.9200),
    "Bettiah": ("Bettiah", BR, 26.8000, 84.5000),
    "Raxaul Junction": ("Raxaul", BR, 26.9800, 84.8500),
    "Siwan Junction": ("Siwan", BR, 26.2200, 84.3600),
    "Begusarai": ("Begusarai", BR, 25.4200, 86.1300),
    "Khagaria Junction": ("Khagaria", BR, 25.5000, 86.4700),
    "Jamalpur Junction": ("Jamalpur", BR, 25.3100, 86.4900),
    "Nawada": ("Nawada", BR, 24.8800, 85.5400),
    "Jehanabad": ("Jehanabad", BR, 25.2100, 84.9900),
    "Jaynagar": ("Jaynagar", BR, 26.5900, 86.1400),
    "Narkatiaganj Junction": ("Narkatiaganj", BR, 27.1000, 84.4600),
    "Ranchi Junction": ("Ranchi", JH, 23.3400, 85.3300),
    "Dhanbad Junction": ("Dhanbad", JH, 23.7900, 86.4300),
    "Tatanagar Junction (Jamshedpur)": ("Jamshedpur", JH, 22.7700, 86.2000),
    "Bokaro Steel City": ("Bokaro", JH, 23.6700, 86.1500),
    "Jasidih Junction": ("Deoghar", JH, 24.5100, 86.6400),
    "Madhupur Junction": ("Madhupur", JH, 24.2700, 86.6400),
    "Koderma Junction": ("Koderma", JH, 24.4700, 85.5900),
    "Deoghar Junction": ("Deoghar", JH, 24.4800, 86.7000),
    "Hatia": ("Ranchi", JH, 23.3000, 85.2800),
    "Barkakana Junction": ("Barkakana", JH, 23.6300, 85.4800),
    "Daltonganj": ("Daltonganj", JH, 24.0400, 84.0700),
    "Chandrapura Junction": ("Chandrapura", JH, 23.7300, 86.0200),
    "Gomoh Junction (NSCB Gomoh)": ("Gomoh", JH, 23.8700, 86.1500),
    "Sahibganj Junction": ("Sahibganj", JH, 25.2500, 87.6400),
    "Dumka": ("Dumka", JH, 24.2700, 87.2500),
    "Garhwa Road Junction": ("Garhwa", JH, 24.1700, 83.8000),
    "Ramgarh Cantt": ("Ramgarh", JH, 23.6300, 85.5600),
    "Chakradharpur": ("Chakradharpur", JH, 22.6800, 85.6300),
    "Ghatsila": ("Ghatsila", JH, 22.5900, 86.4700),
    "Pakur": ("Pakur", JH, 24.6400, 87.8400),
    "Hazaribagh Town": ("Hazaribagh", JH, 23.9900, 85.3600),
    "Latehar": ("Latehar", JH, 23.7400, 84.5000),
    "Lohardaga": ("Lohardaga", JH, 23.4400, 84.6800),
    "Chaibasa": ("Chaibasa", JH, 22.5500, 85.8100),
    "Giridih": ("Giridih", JH, 24.1900, 86.3000),
    "Khastri (Netaji Subhash Chandra Bose Gomoh)": ("Gomoh", JH, 23.8700, 86.1500),
    "Parasnath": ("Parasnath", JH, 23.9600, 86.1400),
    "Kumardhubi": ("Kumardhubi", JH, 23.7900, 86.3700),
    "Godda": ("Godda", JH, 24.8300, 87.2100),
    "Barharwa Junction": ("Barharwa", JH, 24.8600, 87.7700),
}

# Codes already used by the original development seed; kept so an existing database and this file agree.
FIXED_CODES = {"Howrah Junction": "HWH", "Patna Junction": "PNBE", "Siliguri Junction": "SGUJ",
               "Kolkata Sealdah": "SEAL", "Kolkata Railway Station": "KOLK"}
GENERIC_WORDS = {"junction", "railway", "station", "city", "town", "cantt", "road", "steel"}


def make_code(name, taken):
    """Short internal code: the first letters of the station name (not an official railway code)."""
    words = re.findall(r"[A-Za-z]+", name.split("(")[0])
    core = [w for w in words if w.lower() not in GENERIC_WORDS] or words
    letters = "".join(core).upper()
    length = min(4, len(letters))
    while letters[:length] in taken:
        length += 1
    return letters[:length]


def read_sheet(workbook, sheet):
    rows = workbook[sheet].iter_rows(values_only=True)
    header = next(rows)
    return header, [r for r in rows if any(c is not None for c in r)]


def main():
    source = Path(sys.argv[1]) if len(sys.argv) > 1 else DEFAULT_WORKBOOK
    workbook = load_workbook(source, read_only=True, data_only=True)
    OUT_DIR.mkdir(parents=True, exist_ok=True)

    _, station_rows = read_sheet(workbook, "Stations")
    stations = [r[0] for r in station_rows]
    if len(stations) != len(set(stations)):
        sys.exit("Duplicate station names in the Stations sheet")
    missing = [s for s in stations if s not in STATION_REFERENCE]
    extra = [s for s in STATION_REFERENCE if s not in stations]
    if missing or extra:
        sys.exit(f"Station reference table out of sync. Missing: {missing}  Unused: {extra}")

    taken = set(FIXED_CODES.values())
    with open(OUT_DIR / "stations.csv", "w", newline="", encoding="utf-8") as f:
        out = csv.writer(f, lineterminator="\n")
        out.writerow(["name", "code", "city", "state", "latitude", "longitude"])
        for name in stations:
            city, state, lat, lng = STATION_REFERENCE[name]
            code = FIXED_CODES.get(name) or make_code(name, taken)
            taken.add(code)
            out.writerow([name, code, city, state, lat, lng])

    known = set(stations)
    for sheet, filename, columns in (
            ("Train Data", "train_services.csv",
             ["name", "from", "to", "departure", "fare_sl", "fare_1ac", "fare_2ac", "fare_3ac"]),
            ("Bus Data", "bus_services.csv",
             ["name", "from", "to", "departure", "fare_sitting", "fare_sleeper"])):
        _, rows = read_sheet(workbook, sheet)
        names = set()
        with open(OUT_DIR / filename, "w", newline="", encoding="utf-8") as f:
            out = csv.writer(f, lineterminator="\n")
            out.writerow(columns)
            for r in rows:
                if r[1] not in known or r[2] not in known or r[1] == r[2]:
                    sys.exit(f"{sheet}: bad station pair in row {r}")
                if r[0] in names or not re.search(r"\d+$", r[0]):
                    sys.exit(f"{sheet}: service name not unique or has no trailing number: {r[0]}")
                if not re.fullmatch(r"\d\d:\d\d", str(r[3])):
                    sys.exit(f"{sheet}: unexpected time format in row {r}")
                names.add(r[0])
                out.writerow(list(r))
        print(f"{filename}: {len(rows)} services")
    print(f"stations.csv: {len(stations)} stations")


if __name__ == "__main__":
    main()
