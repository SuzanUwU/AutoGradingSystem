# Name:
# Email ID:

def compute_rebate(is_singaporean_household, flat_type, month):
    # Replace the code below with your implementation.
    if not is_singaporean_household:
        return 0.0
    
    if not month in (1, 4, 7, 10):
        return 0.0
    
    if flat_type in ("1", "2"):
        if month == 1:
            return 0.5
        else:
            return 1.0
        
    if flat_type in ("3", "4"):
        if month == 4:
            return 1.0
        else:
            return 0.5
        
    if flat_type in ("5"):
        return 0.5
    
    if flat_type in ("E"):
        if month == 1:
            return 0.0
        else:
            return 0.5