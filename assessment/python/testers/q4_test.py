from q4 import schedule_delivery

print()
print('-' * 20)
print()

print('Test Case 1: schedule_delivery(items)')

print()

items = [
    ("Item 1", "Destination A", "12:30", 15),
    ("Item 2", "Destination B", "13:30", 10),
    ("Item 3", "Destination C", "13:15", 15),
    ("Item 4", "Destination D", "13:45", 20),
]

result = schedule_delivery(items)
print("Expected: [('Item 1', 'Destination A', '12:30', '12:00'), ('Item 3', 'Destination C', '13:15', '12:45'), ('Item 2', 'Destination B', '13:30', '13:10'), ('Item 4', 'Destination D', '13:45', '13:40')]")
print('Actual:   ' + str(result))

print("\nExpected type of returned value: <class 'list'>")
print('Actual type of returned value:   ' + str(type(result)))

print()
print('-' * 20)
print()

##
print('Test Case 2: schedule_delivery(items)')

print()

items = [
    ("Item 1", "Destination A", "12:30", 15),
    ("Item 2", "Destination B", "13:45", 15),
    ("Item 3", "Destination C", "13:15", 30),
    ("Item 4", "Destination D", "13:45", 30),
]

result = schedule_delivery(items)
print("Expected: [('Item 1', 'Destination A', '12:30', '12:00'), ('Item 3', 'Destination C', '13:15', '12:45'), ('Item 2', 'Destination B', '13:45', '13:30'), ('Item 4', 'Destination D', '13:45', '14:15*')]")
print('Actual:   ' + str(result))

print()
print('-' * 20)
print()

