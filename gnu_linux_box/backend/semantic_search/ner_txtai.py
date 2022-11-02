from txtai.pipeline import Entity

data = ["US tops 5 million confirmed virus cases",
        "Canada's last fully intact ice shelf has suddenly collapsed, forming a Manhattan-sized iceberg",
        "Beijing mobilises invasion craft along coast as Taiwan tensions escalate",
        "The National Park Service warns against sacrificing slower friends in a bear attack",
        "Maine man wins $1M from $25 lottery ticket",
        "Make huge profits without work, earn up to $100,000 a day"]

entity = Entity({"path": "dslim/bert-base-NER")

for x, e in enumerate(entity(data)):
  print(data[x])
  print(f"  {e}", "\n")
