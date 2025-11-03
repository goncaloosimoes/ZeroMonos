package tqs.zeromonos.data;

public enum TimeSlot {
    // Time slots definitions matching typical daily periods in a 24-hour format

    EARLY_MORNING, // 06:00 - 08:00
    MORNING, // 08:00 - 12:00
    AFTERNOON, // 12:00 - 16:00
    EVENING, // 16:00 - 20:00
    NIGHT, // 20:00 - 22:00
    LATE_NIGHT, // 22:00 - 06:00
    ANYTIME // Represents any time of the day
}
