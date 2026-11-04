# Name:
# Email ID:

# ###############################################################################
# The function : sort_by_latest_delivery_time() is GIVEN.
# DO NOT remove or modify the function sort_by_latest_delivery_time().
# You MUST use this function in your solution.
# ###############################################################################

def sort_by_latest_delivery_time(items):
    """
    Sorts a list of items in an increasing order of their latest_delivery_time.

    This function takes in a list of items, where each item is a tuple containing
    the item_name, destination, latest_delivery_time, travel_time of item, and sorts 
    them in ascending order of their latest_delivery_time.
    The latest_delivery_time is assumed to be the third element in each item.
    If there is more than one item with the same latest_delivery_time, this function 
    will keep them in the same order as specified in the initial list of items.

    Parameter:
        items (list): A list of items to be sorted.

    Returns: None
        Note that This function sorts the given list items and DOES NOT return a new list.

    """
    for i in range(1, len(items)):
        key = items[i]
        j = i - 1
        while j >= 0 and key[2] < items[j][2]:
            items[j + 1] = items[j]
            j -= 1
        items[j + 1] = key

# ###############################################################################

def timestr_to_minutes(time_str):
    # Convert time in 24-hour format to minutes
    hours, minutes = time_str.split(":")
    return int(hours) * 60 + int(minutes)

def minutes_to_timestr(minutes):
    # Convert minutes to time in 24-hour format
    hours = minutes // 60
    minutes %= 60
    return "{:02d}:{:02d}".format(hours, minutes)

def schedule_delivery(items):
    
    # Convert latest delivery times to minutes
    for i in range(len(items)):
        items[i] = (items[i][0], items[i][1], timestr_to_minutes(items[i][2]), items[i][3])

    # Use custom insertion sort to sort items based on their latest delivery times
    sort_by_latest_delivery_time(items)
    
    # Initialise the schedule
    schedule = []
    
    # Initialise the robot's availability
    robot_availability = 0

    # Assign each item to the robot
    for item in items:
        item_name = item[0]
        destination = item[1]
        latest_delivery_time = item[2]
        travel_time = item[3]
        # item_name, destination, latest_delivery_time, travel_time = item
        
        # Ensure actual delivery time is not earlier than 30 minutes before latest delivery time,
        # taking into account the travel time required.
        earliest_arrival_time = latest_delivery_time - 30
        delivery_time = max(earliest_arrival_time, robot_availability + travel_time)
        
        # Adjust robot availability based on actual delivery time + traveling time back to collection centre
        robot_availability = delivery_time + travel_time

        # Append to the schedule - Indicate late delivery with an '*'
        if delivery_time <= latest_delivery_time:
            schedule.append((item_name, destination, minutes_to_timestr(latest_delivery_time), minutes_to_timestr(delivery_time)))
        else:
            schedule.append((item_name, destination, minutes_to_timestr(latest_delivery_time), minutes_to_timestr(delivery_time)+'*'))

    return schedule