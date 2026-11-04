# Name:
# Email ID:

def is_partially_compliant(table):
    # Replace the code below with your implementation.
    # Let's check the year criteria
    unique_years = []
    for tup in table:
        if tup[1] not in unique_years:
            unique_years.append(tup[1])
    
    # check if students have done their last internship in more than 2 different years
    if len(unique_years) != 1 and len(unique_years) != 2:
        return False
    
    # there is 1 or 2 years in the list. If 1 year, then the year criteria is satisfied.
    if len(unique_years) == 2:
        # check if the 2 years are consecutive
        if not(unique_years[0] == unique_years[1]+1 or unique_years[0] == unique_years[1]-1):
            return False
    
    # Let's check the school criteria now
    schools = ['scis', 'bus', 'acc', 'sosc', 'law', 'eco']
    schools_cnt = [0, 0, 0, 0, 0, 0]
    for tup in table:
        for i in range(len(schools)):
            if tup[2] == schools[i]:
                schools_cnt[i] += 1

    # check school criteria
    for i in range(len(schools_cnt)):
        if schools_cnt[i] > 2:
            return False
   
    # the table satisfies the year and school criteria
    return True