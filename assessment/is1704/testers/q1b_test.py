from q1b import compute_rebate

print()
print('-' * 20)
print()

print('Test Case 1: compute_rebate(True,"1",7)')
print()
result = compute_rebate(True,"1",7)
print('Expected: 1.0')
print('Actual:   ' + str(result))

print("\nExpected type of returned value: <class 'float'>")
print('Actual   type of returned value: ' + str(type(result)))

print()
print('-' * 20)
print()

##
print('Test Case 2: compute_rebate(True,"1",8)')
print()
result = compute_rebate(True,"1",8)
print('Expected: 0.0')
print('Actual:   ' + str(result))

print()
print('-' * 20)
print()

##
print('Test Case 3: compute_rebate(False,"1",10)')
print()
result = compute_rebate(False,"1",10)
print('Expected: 0.0')
print('Actual:   ' + str(result))

print()
print('-' * 20)
print()

##
print('Test Case 4: compute_rebate(True,"2",4)')
print()
result = compute_rebate(True,"2",4)
print('Expected: 1.0')
print('Actual:   ' + str(result))

print()
print('-' * 20)
print()

##
print('Test Case 5: compute_rebate(True,"2",7)')
print()
result = compute_rebate(True,"2",7)
print('Expected: 1.0')
print('Actual:   ' + str(result))

print()
print('-' * 20)
print()

##
print('Test Case 6: compute_rebate(True,"3",1)')
print()
result = compute_rebate(True,"3",1)
print('Expected: 0.5')
print('Actual:   ' + str(result))

print()
print('-' * 20)
print()
      
##
print('Test Case 7: compute_rebate(True,"3",7)')
print()
result = compute_rebate(True,"3",7)
print('Expected: 0.5')
print('Actual:   ' + str(result))

print()
print('-' * 20)
print()

##
print('Test Case 8: compute_rebate(True,"4",10)')
print()
result = compute_rebate(True,"4",10)
print('Expected: 0.5')
print('Actual:   ' + str(result))

print()
print('-' * 20)
print()

##
print('Test Case 9: compute_rebate(True,"4",12)')
print()
result = compute_rebate(True,"4",12)
print('Expected: 0.0')
print('Actual:   ' + str(result))

print()
print('-' * 20)
print()

##
print('Test Case 10: compute_rebate(True,"5",4)')
print()
result = compute_rebate(True,"5",4)
print('Expected: 0.5')
print('Actual:   ' + str(result))

print()
print('-' * 20)
print()

##
print('Test Case 11: compute_rebate(True,"5",11)')
print()
result = compute_rebate(True,"5",11)
print('Expected: 0.0')
print('Actual:   ' + str(result))

print()
print('-' * 20)
print()

##
print('Test Case 12: compute_rebate(True,"E",1)')
print()
result = compute_rebate(True,"E",1)
print('Expected: 0.0')
print('Actual:   ' + str(result))

print()
print('-' * 20)
print()

