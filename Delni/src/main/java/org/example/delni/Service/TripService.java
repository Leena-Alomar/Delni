package org.example.delni.Service;

import lombok.RequiredArgsConstructor;
import org.example.delni.API.ApiException;
import org.example.delni.DTO.In.TripGenerationRequest;
import org.example.delni.DTO.In.TripRequest;
import org.example.delni.DTO.Out.TripItineraryResponse;
import org.example.delni.DTO.Out.TripResponse;
import org.example.delni.Model.City;
import org.example.delni.Model.Place;
import org.example.delni.Model.Trip;
import org.example.delni.Model.TripPlace;
import org.example.delni.Model.User;
import org.example.delni.Repository.CityRepository;
import org.example.delni.Repository.PlaceRepository;
import org.example.delni.Repository.TripPlaceRepository;
import org.example.delni.Repository.TripRepository;
import org.example.delni.Repository.UserRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.Month;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class TripService {

    private static final Set<String> INDOOR_CATEGORIES = Set.of("cafe", "restaurant", "shopping", "entertainment", "museum");
    private static final Set<String> OUTDOOR_CATEGORIES = Set.of("nature", "park", "beach", "adventure");
    private static final List<String> DEFAULT_TIME_SLOT_ORDER = List.of("morning", "noon", "evening");
    private static final DateTimeFormatter TIME_FORMATTER = DateTimeFormatter.ofPattern("HH:mm");

    private final TripRepository tripRepository;
    private final TripPlaceRepository tripPlaceRepository;
    private final PlaceRepository placeRepository;
    private final UserRepository userRepository;
    private final CityRepository cityRepository;
    private final OperatingHoursService operatingHoursService;
    private final WeatherService weatherService;
    private final PrayerTimeService prayerTimeService;

    @Transactional(readOnly = true)
    public List<Trip> getAllTrips() {
        return tripRepository.findAll();
    }

    @Transactional(readOnly = true)
    public List<TripResponse> getAllTripResponses() {
        return tripRepository.findAll().stream()
                .map(this::toTripResponse)
                .toList();
    }

    @Transactional
    public Trip addTrip(Trip trip) {
        return tripRepository.save(trip);
    }

    @Transactional
    public TripResponse addTripResponse(Trip trip) {
        return toTripResponse(addTrip(trip));
    }

    @Transactional
    public TripResponse addTripResponse(TripRequest request) {
        return addTripResponse(toTrip(request));
    }

    @Transactional
    public Trip updateTrip(Trip trip) {
        Trip existingTrip = findTripById(trip.getId());
        mergeIntoExistingTrip(existingTrip, trip);
        return tripRepository.save(existingTrip);
    }

    @Transactional
    public TripResponse updateTripResponse(Trip trip) {
        return toTripResponse(updateTrip(trip));
    }

    @Transactional
    public TripResponse updateTripResponse(Integer id, TripRequest request) {
        Trip existingTrip = findTripById(id);
        applyRequestToTrip(existingTrip, request);
        return toTripResponse(tripRepository.save(existingTrip));
    }

    @Transactional
    public void deleteTrip(Integer id) {
        findTripById(id);
        tripRepository.deleteById(id);
    }

    @Transactional(readOnly = true)
    public Trip findTripById(Integer id) {
        Trip trip = tripRepository.findTripById(id);
        if (trip == null) {
            throw new ApiException("Trip not found");
        }

        trip.setTripPlaces(tripPlaceRepository.findAllByTripIdOrderByDayNumberAscOrderInDayAsc(id));
        return trip;
    }

    @Transactional(readOnly = true)
    public TripResponse getTripResponseById(Integer id) {
        return toTripResponse(findTripById(id));
    }

    @Transactional(readOnly = true)
    public Trip findTripByUserId(Integer userId) {
        return tripRepository.findTripByUserId(userId);
    }

    @Transactional(readOnly = true)
    public Trip findTripByCityId(Integer cityId) {
        return tripRepository.findTripByCityId(cityId);
    }

    @Transactional(readOnly = true)
    public Trip findTripByUserIdAndCityId(Integer userId, Integer cityId) {
        return tripRepository.findTripByUserIdAndCityId(userId, cityId);
    }

    @Transactional
    public Trip generateTrip(TripGenerationRequest request) {
        validateRequest(request);

        User user = userRepository.findById(request.getUserId())
                .orElseThrow(() -> new ApiException("User not found"));
        City city = cityRepository.findById(request.getCityId())
                .orElseThrow(() -> new ApiException("City not found"));
        PlanningContext planningContext = buildPlanningContext(
                city,
                request.getStartDate(),
                request.getEndDate(),
                Boolean.TRUE.equals(request.getWeatherAware()),
                Boolean.TRUE.equals(request.getIncludePrayerSchedule())
        );

        List<Place> candidatePlaces = placeRepository.findAllByCityId(city.getId()).stream()
                .filter(place -> matchesFilter(place.getCategory(), request.getCategory()))
                .filter(place -> matchesFilter(place.getVibeTag(), request.getVibeTag()))
                .sorted(Comparator.<Place>comparingDouble(place -> scorePlaceForRequest(
                        place,
                        request,
                        city,
                        null,
                        null,
                        false,
                        request.getStartDate(),
                        planningContext)).reversed())
                .collect(Collectors.toList());

        if (candidatePlaces.isEmpty()) {
            throw new ApiException("No places available for the selected filters");
        }

        Trip trip = buildTripEntity(request, user, city, planningContext);
        Trip savedTrip = tripRepository.save(trip);

        List<List<Place>> dailyPlan = buildDailyPlan(candidatePlaces, request, city, planningContext);
        List<TripPlace> itinerary = persistItinerary(savedTrip, dailyPlan, request, city, planningContext);

        savedTrip.setTripPlaces(itinerary);
        return savedTrip;
    }

    @Transactional
    public TripResponse generateTripResponse(TripGenerationRequest request) {
        return toTripResponse(generateTrip(request));
    }

    @Transactional(readOnly = true)
    public TripItineraryResponse getTripItinerary(Integer tripId) {
        Trip trip = findTripById(tripId);
        PlanningContext planningContext = buildPlanningContext(
                trip.getCity(),
                trip.getStartDate(),
                trip.getEndDate(),
                Boolean.TRUE.equals(trip.getWeatherAware()),
                Boolean.TRUE.equals(trip.getIncludePrayerSchedule())
        );
        return mapTripToItinerary(trip, planningContext);
    }

    @Transactional
    public TripItineraryResponse replaceTripPlace(Integer tripId, Integer tripPlaceId) {
        Trip trip = findTripById(tripId);
        PlanningContext planningContext = buildPlanningContext(
                trip.getCity(),
                trip.getStartDate(),
                trip.getEndDate(),
                Boolean.TRUE.equals(trip.getWeatherAware()),
                Boolean.TRUE.equals(trip.getIncludePrayerSchedule())
        );

        TripPlace targetTripPlace = trip.getTripPlaces().stream()
                .filter(tripPlace -> tripPlace.getId().equals(tripPlaceId))
                .findFirst()
                .orElseThrow(() -> new ApiException("Trip place not found"));

        TripGenerationRequest requestContext = buildRequestContext(trip);
        LocalDate tripDate = trip.getStartDate().plusDays(targetTripPlace.getDayNumber() - 1L);
        List<TripPlace> sameDayPlaces = trip.getTripPlaces().stream()
                .filter(tripPlace -> tripPlace.getDayNumber().equals(targetTripPlace.getDayNumber()))
                .sorted(Comparator.comparing(TripPlace::getOrderInDay))
                .collect(Collectors.toList());

        TripPlace previousTripPlace = sameDayPlaces.stream()
                .filter(tripPlace -> tripPlace.getOrderInDay() < targetTripPlace.getOrderInDay())
                .max(Comparator.comparing(TripPlace::getOrderInDay))
                .orElse(null);

        TripPlace nextTripPlace = sameDayPlaces.stream()
                .filter(tripPlace -> tripPlace.getOrderInDay() > targetTripPlace.getOrderInDay())
                .min(Comparator.comparing(TripPlace::getOrderInDay))
                .orElse(null);

        Set<Integer> usedPlaceIds = trip.getTripPlaces().stream()
                .map(tripPlace -> tripPlace.getPlace().getId())
                .collect(Collectors.toSet());
        usedPlaceIds.remove(targetTripPlace.getPlace().getId());

        Set<String> usedCategories = sameDayPlaces.stream()
                .filter(tripPlace -> !tripPlace.getId().equals(targetTripPlace.getId()))
                .map(tripPlace -> normalize(tripPlace.getPlace().getCategory()))
                .collect(Collectors.toSet());

        List<Place> availableCandidates = placeRepository.findAllByCityId(trip.getCity().getId()).stream()
                .filter(place -> !usedPlaceIds.contains(place.getId()))
                .collect(Collectors.toList());

        Place replacement = selectReplacementCandidate(
                availableCandidates.stream()
                        .filter(place -> sameTheme(targetTripPlace.getPlace(), place))
                        .collect(Collectors.toList()),
                requestContext,
                trip.getCity(),
                previousTripPlace != null ? previousTripPlace.getPlace() : null,
                tripDate,
                targetTripPlace.getTimeSlot(),
                usedCategories,
                planningContext
        );

        if (replacement == null) {
            replacement = selectReplacementCandidate(
                    availableCandidates,
                    requestContext,
                    trip.getCity(),
                    previousTripPlace != null ? previousTripPlace.getPlace() : null,
                    tripDate,
                    targetTripPlace.getTimeSlot(),
                    usedCategories,
                    planningContext
            );
        }

        if (replacement == null) {
            throw new ApiException("No replacement place available for this stop");
        }

        targetTripPlace.setPlace(replacement);
        targetTripPlace.setActivityDuration(resolveActivityDuration(trip.getTripType(), replacement.getCategory()));
        targetTripPlace.setTravelTimeMins(resolveTravelTime(previousTripPlace != null ? previousTripPlace.getPlace() : null, replacement, trip.getCity()));
        targetTripPlace.setAiNote(buildAiNote(requestContext, replacement, previousTripPlace != null ? previousTripPlace.getPlace() : null, tripDate, targetTripPlace.getTimeSlot(), planningContext));
        tripPlaceRepository.save(targetTripPlace);

        if (nextTripPlace != null) {
            nextTripPlace.setTravelTimeMins(resolveTravelTime(replacement, nextTripPlace.getPlace(), trip.getCity()));
            nextTripPlace.setAiNote(buildAiNote(requestContext, nextTripPlace.getPlace(), replacement, tripDate, nextTripPlace.getTimeSlot(), planningContext));
            tripPlaceRepository.save(nextTripPlace);
        }

        Trip refreshedTrip = findTripById(tripId);
        return mapTripToItinerary(refreshedTrip, planningContext);
    }

    private void validateRequest(TripGenerationRequest request) {
        if (request.getUserId() == null || request.getCityId() == null) {
            throw new ApiException("userId and cityId are required");
        }

        if (request.getStartDate() == null || request.getEndDate() == null) {
            throw new ApiException("Trip dates are required");
        }

        if (request.getEndDate().isBefore(request.getStartDate())) {
            throw new ApiException("End date cannot be before start date");
        }

        int computedDays = (int) ChronoUnit.DAYS.between(request.getStartDate(), request.getEndDate()) + 1;
        if (request.getDaysCount() == null || request.getDaysCount() < 1) {
            request.setDaysCount(computedDays);
        } else if (!request.getDaysCount().equals(computedDays)) {
            request.setDaysCount(computedDays);
        }
    }

    private PlanningContext buildPlanningContext(City city, LocalDate startDate, LocalDate endDate,
                                                 boolean includeWeather, boolean includePrayerSchedule) {
        return new PlanningContext(
                includeWeather ? weatherService.fetchForecast(city, startDate, endDate) : ForecastContext.empty(),
                includePrayerSchedule ? prayerTimeService.fetchPrayerTimes(city, startDate, endDate) : PrayerContext.empty()
        );
    }

    private List<List<Place>> buildDailyPlan(List<Place> candidates, TripGenerationRequest request, City city, PlanningContext planningContext) {
        int days = Math.max(1, request.getDaysCount());
        int placesPerDay = resolvePlacesPerDay(request);
        int totalPlacesTarget = Math.min(candidates.size(), Math.max(days, days * placesPerDay));
        List<Place> remainingPlaces = new ArrayList<>(candidates);
        List<List<Place>> dailyPlan = new ArrayList<>();
        int selectedCount = 0;

        for (int dayNumber = 1; dayNumber <= days && selectedCount < totalPlacesTarget; dayNumber++) {
            int daysLeft = days - dayNumber + 1;
            int remainingSlots = totalPlacesTarget - selectedCount;
            int dailyTarget = Math.max(1, (int) Math.ceil(remainingSlots / (double) daysLeft));
            LocalDate itineraryDate = request.getStartDate().plusDays(dayNumber - 1L);
            List<Place> dayPlan = new ArrayList<>();
            Set<String> usedCategories = new HashSet<>();
            Place previousPlace = null;

            for (int order = 1; order <= dailyTarget && !remainingPlaces.isEmpty(); order++) {
                String timeSlot = resolveTimeSlot(order, dailyTarget);
                Place selected = pickBestPlace(remainingPlaces, previousPlace, city, request, itineraryDate, timeSlot, usedCategories, planningContext);
                if (selected == null) {
                    break;
                }

                dayPlan.add(selected);
                usedCategories.add(normalize(selected.getCategory()));
                remainingPlaces.remove(selected);
                previousPlace = selected;
                selectedCount++;
            }

            dailyPlan.add(dayPlan);
        }

        return dailyPlan;
    }

    private Place pickBestPlace(List<Place> candidates, Place previousPlace, City city,
                                TripGenerationRequest request, LocalDate date, String timeSlot,
                                Set<String> usedCategories, PlanningContext planningContext) {
        return candidates.stream()
                .max(Comparator.comparingDouble(place ->
                        scorePlaceForRequest(place, request, city, previousPlace, timeSlot,
                                usedCategories.contains(normalize(place.getCategory())), date, planningContext)
                                + availabilityAdjustment(place, request, date, timeSlot, planningContext)))
                .orElse(null);
    }

    private List<TripPlace> persistItinerary(Trip trip, List<List<Place>> dailyPlan, TripGenerationRequest request,
                                             City city, PlanningContext planningContext) {
        List<TripPlace> itinerary = new ArrayList<>();

        for (int dayIndex = 0; dayIndex < dailyPlan.size(); dayIndex++) {
            List<Place> dayPlaces = dailyPlan.get(dayIndex);
            LocalDate itineraryDate = request.getStartDate().plusDays(dayIndex);
            Place previousPlace = null;

            for (int order = 0; order < dayPlaces.size(); order++) {
                Place currentPlace = dayPlaces.get(order);
                String timeSlot = resolveTimeSlot(order + 1, dayPlaces.size());

                TripPlace tripPlace = new TripPlace();
                tripPlace.setTrip(trip);
                tripPlace.setPlace(currentPlace);
                tripPlace.setDayNumber(dayIndex + 1);
                tripPlace.setOrderInDay(order + 1);
                tripPlace.setTimeSlot(timeSlot);
                tripPlace.setActivityDuration(resolveActivityDuration(request.getTripType(), currentPlace.getCategory()));
                tripPlace.setTravelTimeMins(resolveTravelTime(previousPlace, currentPlace, city));
                tripPlace.setAiNote(buildAiNote(request, currentPlace, previousPlace, itineraryDate, timeSlot, planningContext));
                itinerary.add(tripPlaceRepository.save(tripPlace));
                previousPlace = currentPlace;
            }
        }

        return itinerary;
    }

    private TripItineraryResponse mapTripToItinerary(Trip trip, PlanningContext planningContext) {
        List<TripPlace> orderedTripPlaces = trip.getTripPlaces() == null
                ? new ArrayList<>()
                : trip.getTripPlaces().stream()
                .sorted(Comparator.comparing(TripPlace::getDayNumber).thenComparing(TripPlace::getOrderInDay))
                .collect(Collectors.toList());

        List<TripItineraryResponse.MapPoint> mapPoints = orderedTripPlaces.stream()
                .map(tripPlace -> new TripItineraryResponse.MapPoint(
                        tripPlace.getPlace().getId(),
                        tripPlace.getPlace().getName(),
                        tripPlace.getPlace().getLatitude(),
                        tripPlace.getPlace().getLongitude(),
                        tripPlace.getDayNumber(),
                        tripPlace.getOrderInDay(),
                        tripPlace.getTimeSlot()))
                .collect(Collectors.toList());

        List<TripItineraryResponse.TripDay> days = new ArrayList<>();
        int totalDays = trip.getDaysCount() == null ? orderedTripPlaces.stream()
                .map(TripPlace::getDayNumber)
                .max(Integer::compareTo)
                .orElse(0) : trip.getDaysCount();

        for (int dayNumber = 1; dayNumber <= totalDays; dayNumber++) {
            int currentDay = dayNumber;
            LocalDate tripDate = trip.getStartDate() != null ? trip.getStartDate().plusDays(dayNumber - 1L) : null;
            List<TripPlace> dayPlaces = orderedTripPlaces.stream()
                    .filter(tripPlace -> tripPlace.getDayNumber().equals(currentDay))
                    .collect(Collectors.toList());
            Map<Integer, LocalTime> plannedTimes = tripDate != null
                    ? buildPlannedTimes(dayPlaces, tripDate, planningContext)
                    : new LinkedHashMap<>();

            List<TripItineraryResponse.TimeBlock> blocks = new ArrayList<>();
            for (String timeSlot : resolveTimeSlotsForDay(dayPlaces)) {
                LocalTime slotTime = tripDate != null
                        ? resolveRepresentativeTime(tripDate, timeSlot, planningContext)
                        : resolveRepresentativeTimeFallback(timeSlot);

                List<TripItineraryResponse.TripStop> stops = dayPlaces.stream()
                        .filter(tripPlace -> normalize(tripPlace.getTimeSlot()).equals(normalize(timeSlot)))
                        .map(tripPlace -> new TripItineraryResponse.TripStop(
                                tripPlace.getId(),
                                tripPlace.getPlace().getId(),
                                tripPlace.getOrderInDay(),
                                formatTime(plannedTimes.getOrDefault(tripPlace.getId(), slotTime)),
                                tripPlace.getPlace().getName(),
                                tripPlace.getPlace().getCategory(),
                                tripPlace.getPlace().getImageUrl(),
                                tripDate != null ? resolveStatusLabel(
                                        tripPlace.getPlace(),
                                        tripDate,
                                        plannedTimes.getOrDefault(tripPlace.getId(), slotTime),
                                        planningContext) : "Planned",
                                tripPlace.getPlace().getGoogleRating(),
                                tripPlace.getPlace().getIsTrending(),
                                tripPlace.getActivityDuration(),
                                tripPlace.getTravelTimeMins(),
                                tripPlace.getAiNote(),
                                tripPlace.getPlace().getLatitude(),
                                tripPlace.getPlace().getLongitude(),
                                true,
                                buildPlaceSubtitle(tripPlace.getPlace()),
                                buildMapsUrl(tripPlace.getPlace())
                        ))
                        .collect(Collectors.toList());

                if (!stops.isEmpty()) {
                    blocks.add(new TripItineraryResponse.TimeBlock(
                            timeSlot,
                            stops.get(0).getPlannedTime(),
                            Boolean.TRUE.equals(trip.getWeatherAware()) ? buildWeatherNote(tripDate, timeSlot, planningContext) : null,
                            Boolean.TRUE.equals(trip.getIncludePrayerSchedule()) ? buildPrayerNote(tripDate, timeSlot, planningContext) : null,
                            stops.size(),
                            stops
                    ));
                }
            }

            days.add(new TripItineraryResponse.TripDay(
                    dayNumber,
                    "Day " + dayNumber,
                    tripDate,
                    Boolean.TRUE.equals(trip.getWeatherAware()) ? createWeatherSummary(tripDate, planningContext) : null,
                    Boolean.TRUE.equals(trip.getIncludePrayerSchedule()) ? createPrayerSchedule(tripDate, planningContext) : null,
                    blocks
            ));
        }

        return new TripItineraryResponse(
                trip.getId(),
                trip.getTitle(),
                trip.getCity() != null ? trip.getCity().getName() : null,
                trip.getTripType(),
                trip.getGroupSize(),
                trip.getStartDate(),
                trip.getEndDate(),
                trip.getDaysCount(),
                trip.getTotalCostEstimate(),
                trip.getIncludeTikTokTrending(),
                trip.getWeatherAware(),
                trip.getIncludePrayerSchedule(),
                trip.getIncludeTopRatings(),
                buildHeaderTitle(trip),
                buildHeaderSubtitle(trip),
                buildPlanningSummary(trip, orderedTripPlaces),
                buildQuickNotes(trip, orderedTripPlaces),
                new TripItineraryResponse.Summary(
                        trip.getCity() != null ? trip.getCity().getName() : null,
                        trip.getDaysCount(),
                        trip.getTotalCostEstimate()
                ),
                days,
                mapPoints
        );
    }

    private double scorePlaceForRequest(Place place, TripGenerationRequest request, City city,
                                        Place previousPlace, String timeSlot, boolean repeatedCategory,
                                        LocalDate date, PlanningContext planningContext) {
        double score = 0;
        double smartScore = valueOrZero(place.getSmartScore());
        double googleRating = valueOrZero(place.getGoogleRating());
        double trendScore = Math.min(valueOrZero(place.getTiktokTrendScore()), 100);

        score += smartScore * 4;
        score += googleRating * 3;
        score += trendScore * 0.35;

        if (Boolean.TRUE.equals(request.getIncludeTikTokTrending())) {
            score += Boolean.TRUE.equals(place.getIsTrending()) ? 18 : -12;
        }

        if (Boolean.TRUE.equals(request.getIncludeTopRatings())) {
            score += googleRating * 2;
        }

        if (Boolean.TRUE.equals(request.getWeatherAware())) {
            score += weatherSuitabilityScore(place, date, timeSlot, planningContext);
        }

        if (Boolean.TRUE.equals(request.getIncludePrayerSchedule())) {
            score += prayerSuitabilityScore(date, timeSlot, planningContext);
        }

        score += tripTypeCategoryBoost(request.getTripType(), place.getCategory());
        score += budgetCategoryBoost(request.getBudgetTier(), place.getCategory());

        if (place.getOperatingHours() != null && !place.getOperatingHours().isEmpty()) {
            score += 4;
        }

        if (repeatedCategory) {
            score -= 6;
        }

        if (previousPlace == null) {
            score -= haversine(city.getLatitude(), city.getLongitude(), place.getLatitude(), place.getLongitude()) * 0.2;
        } else {
            score -= haversine(previousPlace.getLatitude(), previousPlace.getLongitude(), place.getLatitude(), place.getLongitude()) * 0.8;
        }

        if ("evening".equalsIgnoreCase(timeSlot) && isEveningFriendly(place.getCategory())) {
            score += 5;
        }

        return score;
    }

    private double availabilityAdjustment(Place place, TripGenerationRequest request, LocalDate date, String timeSlot,
                                          PlanningContext planningContext) {
        LocalTime slotTime = resolveRepresentativeTime(date, timeSlot, planningContext);
        boolean openAtRequestedTime = isPlaceOpenAt(place, date, slotTime, planningContext);
        double adjustment = openAtRequestedTime ? 8 : -10;

        if (Boolean.TRUE.equals(request.getIncludePrayerSchedule())) {
            adjustment += prayerWindowAdjustment(date, slotTime, planningContext);
        }

        if (Boolean.TRUE.equals(request.getWeatherAware())) {
            adjustment += weatherSlotAdjustment(place, date, slotTime, planningContext);
        }

        return adjustment;
    }

    private boolean matchesFilter(String actualValue, String requestedValue) {
        if (requestedValue == null || requestedValue.isBlank()) {
            return true;
        }

        return normalize(actualValue).equals(normalize(requestedValue));
    }

    private Trip buildTripEntity(TripGenerationRequest request, User user, City city, PlanningContext planningContext) {
        Trip trip = new Trip();
        trip.setTitle(request.getTitle() == null || request.getTitle().isBlank()
                ? "Trip to " + city.getName()
                : request.getTitle());
        trip.setStartDate(request.getStartDate());
        trip.setEndDate(request.getEndDate());
        trip.setDaysCount(request.getDaysCount());
        trip.setGroupSize(request.getGroupSize() == null ? 1 : request.getGroupSize());
        trip.setTripType(defaultTripType(request.getTripType()));
        trip.setBudgetTier(defaultBudgetTier(request.getBudgetTier()));
        trip.setTotalCostEstimate(request.getTotalCostEstimate());
        trip.setIncludeTikTokTrending(Boolean.TRUE.equals(request.getIncludeTikTokTrending()));
        trip.setWeatherAware(Boolean.TRUE.equals(request.getWeatherAware()));
        trip.setIncludePrayerSchedule(Boolean.TRUE.equals(request.getIncludePrayerSchedule()));
        trip.setIncludeTopRatings(Boolean.TRUE.equals(request.getIncludeTopRatings()));
        trip.setUserPrompt(request.getUserPrompt());
        trip.setAiItineraryLogic(buildAiLogicSummary(request, planningContext));
        trip.setUser(user);
        trip.setCity(city);
        return trip;
    }

    private int resolvePlacesPerDay(TripGenerationRequest request) {
        String tripType = defaultTripType(request.getTripType());
        if ("Family".equalsIgnoreCase(tripType)) {
            return 2;
        }

        if ("Solo".equalsIgnoreCase(tripType)) {
            return 3;
        }

        return 3;
    }

    private String resolveTimeSlot(int orderInDay, int placesInDay) {
        if (placesInDay <= 1) {
            return "noon";
        }

        if (placesInDay == 2) {
            return orderInDay == 1 ? "morning" : "evening";
        }

        if (orderInDay == 1) {
            return "morning";
        }

        if (orderInDay == placesInDay) {
            return "evening";
        }

        return "noon";
    }

    private int resolveActivityDuration(String tripType, String category) {
        String normalizedTripType = defaultTripType(tripType);
        if ("Family".equalsIgnoreCase(normalizedTripType)) {
            return "entertainment".equalsIgnoreCase(category) ? 120 : 90;
        }

        if ("Couple".equalsIgnoreCase(normalizedTripType)) {
            return "restaurant".equalsIgnoreCase(category) ? 110 : 90;
        }

        if ("Solo".equalsIgnoreCase(normalizedTripType)) {
            return 75;
        }

        return 90;
    }

    private Integer resolveTravelTime(Place previousPlace, Place currentPlace, City city) {
        if (currentPlace == null) {
            return 0;
        }

        double distanceKm;
        if (previousPlace == null) {
            distanceKm = haversine(city.getLatitude(), city.getLongitude(), currentPlace.getLatitude(), currentPlace.getLongitude());
        } else {
            distanceKm = haversine(previousPlace.getLatitude(), previousPlace.getLongitude(),
                    currentPlace.getLatitude(), currentPlace.getLongitude());
        }

        return (int) Math.max(5, Math.round((distanceKm / 35.0) * 60.0));
    }

    private boolean isPlaceOpenAt(Place place, LocalDate date, LocalTime candidateTime, PlanningContext planningContext) {
        PrayerDay prayerDay = planningContext != null ? planningContext.getPrayerDay(date) : null;
        boolean prayerBreakActive = prayerDay != null
                ? prayerDay.isNearPrayerWindow(candidateTime)
                : isFallbackFridayPrayerWindow(date.getDayOfWeek(), candidateTime);
        return operatingHoursService.isOpenAt(place, date, candidateTime, prayerBreakActive);
    }

    private boolean isFallbackFridayPrayerWindow(DayOfWeek dayOfWeek, LocalTime candidateTime) {
        return dayOfWeek == DayOfWeek.FRIDAY
                && candidateTime != null
                && candidateTime.isAfter(LocalTime.NOON)
                && candidateTime.isBefore(LocalTime.of(14, 30));
    }

    private LocalTime resolveRepresentativeTime(LocalDate date, String timeSlot, PlanningContext planningContext) {
        PrayerDay prayerDay = planningContext.getPrayerDay(date);
        if (prayerDay != null) {
            return prayerDay.recommendedTimeForSlot(timeSlot);
        }

        return resolveRepresentativeTimeFallback(timeSlot);
    }

    private LocalTime resolveRepresentativeTimeFallback(String timeSlot) {
        return switch (normalize(timeSlot)) {
            case "morning" -> LocalTime.of(9, 30);
            case "evening" -> LocalTime.of(19, 0);
            default -> LocalTime.of(14, 0);
        };
    }

    private double weatherSuitabilityScore(Place place, LocalDate date, String timeSlot, PlanningContext planningContext) {
        WeatherHour weatherHour = planningContext.getWeatherHour(date, resolveRepresentativeTime(date, timeSlot, planningContext));
        if (weatherHour != null) {
            return calculateWeatherCategoryScore(place.getCategory(), weatherHour.getApparentTemperature() != null
                    ? weatherHour.getApparentTemperature() : weatherHour.getTemperature(), weatherHour.getPrecipitationProbability(), weatherHour.getWeatherCode());
        }

        WeatherDay weatherDay = planningContext.getWeatherDay(date);
        if (weatherDay != null) {
            Double referenceTemperature = weatherDay.getMaxTemperature() != null
                    ? weatherDay.getMaxTemperature()
                    : weatherDay.getMinTemperature();
            return calculateWeatherCategoryScore(place.getCategory(), referenceTemperature, weatherDay.getPrecipitationProbability(), weatherDay.getWeatherCode());
        }

        return fallbackWeatherSuitabilityScore(place, date);
    }

    private double calculateWeatherCategoryScore(String category, Double temperature, Integer precipitationProbability, Integer weatherCode) {
        String normalizedCategory = normalize(category);
        double normalizedTemperature = temperature == null ? 28.0 : temperature;
        int normalizedPrecipitation = precipitationProbability == null ? 0 : precipitationProbability;
        boolean harshWeather = normalizedTemperature >= 36 || normalizedTemperature <= 10 || normalizedPrecipitation >= 50 || isSevereWeatherCode(weatherCode);
        boolean outdoorFriendly = normalizedTemperature >= 18 && normalizedTemperature <= 32 && normalizedPrecipitation < 35 && !isSevereWeatherCode(weatherCode);

        if (OUTDOOR_CATEGORIES.contains(normalizedCategory)) {
            if (outdoorFriendly) {
                return 12;
            }
            if (harshWeather) {
                return -14;
            }
            return -3;
        }

        if (INDOOR_CATEGORIES.contains(normalizedCategory)) {
            if (harshWeather) {
                return 10;
            }
            if (outdoorFriendly) {
                return 2;
            }
            return 5;
        }

        return harshWeather ? -4 : 2;
    }

    private boolean isSevereWeatherCode(Integer weatherCode) {
        return weatherCode != null && (weatherCode >= 51 || weatherCode == 45 || weatherCode == 48);
    }

    private double fallbackWeatherSuitabilityScore(Place place, LocalDate date) {
        String category = normalize(place.getCategory());
        Month month = date.getMonth();
        boolean hotSeason = month.getValue() >= 5 && month.getValue() <= 9;

        if (hotSeason) {
            if (INDOOR_CATEGORIES.contains(category)) {
                return 10;
            }

            if (OUTDOOR_CATEGORIES.contains(category)) {
                return -7;
            }
        } else {
            if (OUTDOOR_CATEGORIES.contains(category)) {
                return 8;
            }

            if (INDOOR_CATEGORIES.contains(category)) {
                return 3;
            }
        }

        return 0;
    }

    private double prayerSuitabilityScore(LocalDate date, String timeSlot, PlanningContext planningContext) {
        LocalTime slotTime = resolveRepresentativeTime(date, timeSlot, planningContext);
        return prayerWindowAdjustment(date, slotTime, planningContext);
    }

    private double prayerWindowAdjustment(LocalDate date, LocalTime slotTime, PlanningContext planningContext) {
        PrayerDay prayerDay = planningContext.getPrayerDay(date);
        if (prayerDay == null) {
            return 0;
        }

        return prayerDay.isNearPrayerWindow(slotTime) ? -12 : 6;
    }

    private double weatherSlotAdjustment(Place place, LocalDate date, LocalTime slotTime, PlanningContext planningContext) {
        WeatherHour weatherHour = planningContext.getWeatherHour(date, slotTime);
        if (weatherHour == null) {
            return 0;
        }

        if (OUTDOOR_CATEGORIES.contains(normalize(place.getCategory()))) {
            return Boolean.TRUE.equals(weatherHour.getOutdoorFriendly()) ? 6 : -8;
        }

        if (INDOOR_CATEGORIES.contains(normalize(place.getCategory()))) {
            return Boolean.TRUE.equals(weatherHour.getHarshWeather()) ? 4 : 1;
        }

        return 0;
    }

    private double tripTypeCategoryBoost(String tripType, String category) {
        String normalizedTripType = normalize(defaultTripType(tripType));
        String normalizedCategory = normalize(category);

        return switch (normalizedTripType) {
            case "family" -> Set.of("entertainment", "nature", "history").contains(normalizedCategory) ? 8 : 2;
            case "solo" -> Set.of("history", "cafe", "nature").contains(normalizedCategory) ? 7 : 2;
            case "couple" -> Set.of("restaurant", "cafe", "nature").contains(normalizedCategory) ? 8 : 2;
            default -> Set.of("entertainment", "shopping", "cafe").contains(normalizedCategory) ? 7 : 2;
        };
    }

    private double budgetCategoryBoost(String budgetTier, String category) {
        String normalizedBudget = normalize(defaultBudgetTier(budgetTier));
        String normalizedCategory = normalize(category);

        return switch (normalizedBudget) {
            case "low", "economy" -> Set.of("nature", "history", "cafe").contains(normalizedCategory) ? 6 : 0;
            case "high", "luxury" -> Set.of("shopping", "restaurant", "entertainment").contains(normalizedCategory) ? 6 : 1;
            default -> 2;
        };
    }

    private boolean isEveningFriendly(String category) {
        return Set.of("restaurant", "cafe", "shopping", "entertainment").contains(normalize(category));
    }

    private double haversine(Double lat1, Double lon1, Double lat2, Double lon2) {
        if (lat1 == null || lon1 == null || lat2 == null || lon2 == null) {
            return 0;
        }

        final double earthRadiusKm = 6371.0;
        double dLat = Math.toRadians(lat2 - lat1);
        double dLon = Math.toRadians(lon2 - lon1);
        double originLat = Math.toRadians(lat1);
        double destinationLat = Math.toRadians(lat2);

        double a = Math.sin(dLat / 2) * Math.sin(dLat / 2)
                + Math.sin(dLon / 2) * Math.sin(dLon / 2) * Math.cos(originLat) * Math.cos(destinationLat);
        double c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));
        return earthRadiusKm * c;
    }

    private String buildAiNote(TripGenerationRequest request, Place place, Place previousPlace,
                               LocalDate date, String timeSlot, PlanningContext planningContext) {
        List<String> reasons = new ArrayList<>();
        reasons.add(valueOrZero(place.getSmartScore()) > 0 ? "high smart score" : "balanced match");

        if (Boolean.TRUE.equals(place.getIsTrending())) {
            reasons.add("currently trending");
        }

        if (Boolean.TRUE.equals(request.getIncludeTopRatings()) && valueOrZero(place.getGoogleRating()) >= 4.0) {
            reasons.add("strong Google rating");
        }

        if (Boolean.TRUE.equals(request.getWeatherAware())) {
            WeatherHour weatherHour = planningContext.getWeatherHour(date, resolveRepresentativeTime(date, timeSlot, planningContext));
            if (weatherHour != null) {
                reasons.add("weather checked: " + weatherHour.getSummary().toLowerCase(Locale.ROOT));
            } else {
                double weatherScore = fallbackWeatherSuitabilityScore(place, date);
                reasons.add(weatherScore >= 0 ? "fits the expected weather" : "included despite weather tradeoff");
            }
        }

        if (Boolean.TRUE.equals(request.getIncludePrayerSchedule())) {
            PrayerDay prayerDay = planningContext.getPrayerDay(date);
            if (prayerDay != null) {
                reasons.add(prayerDay.isNearPrayerWindow(resolveRepresentativeTime(date, timeSlot, planningContext))
                        ? "scheduled around prayer timing constraints"
                        : "aligned with prayer schedule");
            } else {
                reasons.add("prayer-aware timing requested");
            }
        }

        if (isPlaceOpenAt(place, date, resolveRepresentativeTime(date, timeSlot, planningContext), planningContext)) {
            reasons.add("open during the selected time slot");
        }

        if (previousPlace != null) {
            reasons.add("sequenced to reduce travel time");
        }

        return "Selected for " + String.join(", ", reasons);
    }

    private String buildAiLogicSummary(TripGenerationRequest request, PlanningContext planningContext) {
        return "Ranked places using smart score, Google rating, TikTok trend score, budget fit, trip type fit, "
                + "weather suitability, operating hours, prayer timing, and geographic proximity"
                + (Boolean.TRUE.equals(request.getWeatherAware()) && planningContext.getWeatherForecast().isAvailable()
                ? "; live weather forecast applied" : "")
                + (Boolean.TRUE.equals(request.getIncludePrayerSchedule()) && planningContext.getPrayerContext().isAvailable()
                ? "; live prayer schedule applied" : "")
                + (request.getUserPrompt() != null && !request.getUserPrompt().isBlank() ? "; prompt: " + request.getUserPrompt() : "");
    }

    private String defaultTripType(String tripType) {
        if (tripType == null || tripType.isBlank()) {
            return "Friends";
        }

        return capitalize(normalize(tripType));
    }

    private String defaultBudgetTier(String budgetTier) {
        if (budgetTier == null || budgetTier.isBlank()) {
            return "Medium";
        }

        String normalized = normalize(budgetTier);
        return switch (normalized) {
            case "economy", "low" -> "Low";
            case "luxury", "high" -> "High";
            default -> "Medium";
        };
    }

    private TripGenerationRequest buildRequestContext(Trip trip) {
        TripGenerationRequest request = new TripGenerationRequest();
        request.setTitle(trip.getTitle());
        request.setStartDate(trip.getStartDate());
        request.setEndDate(trip.getEndDate());
        request.setDaysCount(trip.getDaysCount());
        request.setGroupSize(trip.getGroupSize());
        request.setTripType(trip.getTripType());
        request.setBudgetTier(trip.getBudgetTier());
        request.setTotalCostEstimate(trip.getTotalCostEstimate());
        request.setIncludeTikTokTrending(trip.getIncludeTikTokTrending());
        request.setWeatherAware(trip.getWeatherAware());
        request.setIncludePrayerSchedule(trip.getIncludePrayerSchedule());
        request.setIncludeTopRatings(trip.getIncludeTopRatings());
        request.setUserPrompt(trip.getUserPrompt());
        return request;
    }

    private Place selectReplacementCandidate(List<Place> candidates, TripGenerationRequest request, City city,
                                             Place previousPlace, LocalDate date, String timeSlot,
                                             Set<String> usedCategories, PlanningContext planningContext) {
        if (candidates.isEmpty()) {
            return null;
        }

        return candidates.stream()
                .max(Comparator.comparingDouble(place ->
                        scorePlaceForRequest(place, request, city, previousPlace, timeSlot,
                                usedCategories.contains(normalize(place.getCategory())), date, planningContext)
                                + availabilityAdjustment(place, request, date, timeSlot, planningContext)))
                .orElse(null);
    }

    private boolean sameTheme(Place originalPlace, Place candidatePlace) {
        if (candidatePlace.getId().equals(originalPlace.getId())) {
            return false;
        }

        boolean matchesCategory = originalPlace.getCategory() != null && !originalPlace.getCategory().isBlank()
                && matchesFilter(candidatePlace.getCategory(), originalPlace.getCategory());
        boolean matchesVibe = originalPlace.getVibeTag() != null && !originalPlace.getVibeTag().isBlank()
                && matchesFilter(candidatePlace.getVibeTag(), originalPlace.getVibeTag());

        return matchesCategory || matchesVibe;
    }

    private List<String> resolveTimeSlotsForDay(List<TripPlace> dayPlaces) {
        List<String> orderedSlots = new ArrayList<>(DEFAULT_TIME_SLOT_ORDER);

        dayPlaces.stream()
                .map(TripPlace::getTimeSlot)
                .filter(slot -> slot != null && !slot.isBlank())
                .map(this::normalize)
                .filter(slot -> !orderedSlots.contains(slot))
                .forEach(orderedSlots::add);

        return orderedSlots;
    }

    private Map<Integer, LocalTime> buildPlannedTimes(List<TripPlace> dayPlaces, LocalDate tripDate, PlanningContext planningContext) {
        Map<Integer, LocalTime> plannedTimes = new LinkedHashMap<>();
        TripPlace previousTripPlace = null;
        String previousSlot = null;
        LocalTime currentStart = null;

        for (TripPlace tripPlace : dayPlaces.stream()
                .sorted(Comparator.comparing(TripPlace::getOrderInDay))
                .toList()) {
            LocalTime slotStart = resolveRepresentativeTime(tripDate, tripPlace.getTimeSlot(), planningContext);
            if (previousTripPlace == null || !normalize(previousSlot).equals(normalize(tripPlace.getTimeSlot()))) {
                currentStart = slotStart;
            } else {
                currentStart = currentStart
                        .plusMinutes(previousTripPlace.getActivityDuration() != null ? previousTripPlace.getActivityDuration() : 90)
                        .plusMinutes(tripPlace.getTravelTimeMins() != null ? tripPlace.getTravelTimeMins() : 0);
                if (currentStart.isBefore(slotStart)) {
                    currentStart = slotStart;
                }
            }

            plannedTimes.put(tripPlace.getId(), currentStart);
            previousTripPlace = tripPlace;
            previousSlot = tripPlace.getTimeSlot();
        }

        return plannedTimes;
    }

    private String resolveStatusLabel(Place place, LocalDate date, LocalTime plannedTime, PlanningContext planningContext) {
        if (place.getOperatingHours() == null || place.getOperatingHours().isEmpty()) {
            return "Check hours";
        }

        return isPlaceOpenAt(place, date, plannedTime, planningContext)
                ? "Open"
                : "Closed at planned time";
    }

    private String buildWeatherNote(LocalDate date, String timeSlot, PlanningContext planningContext) {
        if (date == null) {
            return null;
        }

        WeatherHour weatherHour = planningContext.getWeatherHour(date, resolveRepresentativeTime(date, timeSlot, planningContext));
        if (weatherHour != null) {
            String temperatureText = weatherHour.getApparentTemperature() != null
                    ? Math.round(weatherHour.getApparentTemperature()) + "C"
                    : weatherHour.getTemperature() != null ? Math.round(weatherHour.getTemperature()) + "C" : "";
            return weatherHour.getSummary() + (temperatureText.isBlank() ? "" : " around " + temperatureText);
        }

        WeatherDay weatherDay = planningContext.getWeatherDay(date);
        if (weatherDay != null) {
            return weatherDay.getSummary();
        }

        return null;
    }

    private String buildPrayerNote(LocalDate date, String timeSlot, PlanningContext planningContext) {
        if (date == null) {
            return null;
        }

        PrayerDay prayerDay = planningContext.getPrayerDay(date);
        if (prayerDay == null) {
            return null;
        }

        LocalTime slotTime = resolveRepresentativeTime(date, timeSlot, planningContext);
        String nearbyPrayer = prayerDay.nearbyPrayerLabel(slotTime);
        return nearbyPrayer != null
                ? "Adjusted around " + nearbyPrayer + " prayer"
                : "Prayer-aware slot";
    }

    private TripItineraryResponse.WeatherSummary createWeatherSummary(LocalDate date, PlanningContext planningContext) {
        if (date == null) {
            return null;
        }

        WeatherDay weatherDay = planningContext.getWeatherDay(date);
        if (weatherDay == null) {
            return null;
        }

        return new TripItineraryResponse.WeatherSummary(
                weatherDay.getSummary(),
                weatherDay.getMinTemperature(),
                weatherDay.getMaxTemperature(),
                weatherDay.getPrecipitationProbability(),
                weatherDay.getOutdoorFriendly()
        );
    }

    private TripItineraryResponse.PrayerSchedule createPrayerSchedule(LocalDate date, PlanningContext planningContext) {
        if (date == null) {
            return null;
        }

        PrayerDay prayerDay = planningContext.getPrayerDay(date);
        if (prayerDay == null) {
            return null;
        }

        return new TripItineraryResponse.PrayerSchedule(
                prayerDay.getMethodName(),
                formatTime(prayerDay.getFajr()),
                formatTime(prayerDay.getSunrise()),
                formatTime(prayerDay.getDhuhr()),
                formatTime(prayerDay.getAsr()),
                formatTime(prayerDay.getMaghrib()),
                formatTime(prayerDay.getIsha())
        );
    }

    private String formatTime(LocalTime localTime) {
        return localTime == null ? null : localTime.format(TIME_FORMATTER);
    }

    private String normalize(String value) {
        return value == null ? "" : value.trim().toLowerCase(Locale.ROOT);
    }

    private String capitalize(String value) {
        if (value == null || value.isBlank()) {
            return value;
        }

        return value.substring(0, 1).toUpperCase(Locale.ROOT) + value.substring(1);
    }

    private double valueOrZero(Double value) {
        return value == null ? 0 : value;
    }

    private String buildHeaderTitle(Trip trip) {
        String cityName = trip.getCity() != null ? trip.getCity().getName() : "Your city";
        int daysCount = trip.getDaysCount() != null ? trip.getDaysCount() : 0;
        return cityName + " trip plan" + (daysCount > 0 ? " • " + daysCount + " day(s)" : "");
    }

    private String buildHeaderSubtitle(Trip trip) {
        List<String> parts = new ArrayList<>();
        if (trip.getTripType() != null) {
            parts.add(trip.getTripType());
        }
        if (trip.getGroupSize() != null) {
            parts.add("Group of " + trip.getGroupSize());
        }
        if (trip.getBudgetTier() != null) {
            parts.add(trip.getBudgetTier() + " budget");
        }
        return parts.isEmpty() ? "Generated itinerary" : String.join(" • ", parts);
    }

    private String buildPlanningSummary(Trip trip, List<TripPlace> orderedTripPlaces) {
        int totalStops = orderedTripPlaces != null ? orderedTripPlaces.size() : 0;
        long trendingStops = orderedTripPlaces == null ? 0 : orderedTripPlaces.stream()
                .filter(tripPlace -> Boolean.TRUE.equals(tripPlace.getPlace().getIsTrending()))
                .count();
        return totalStops + " stop(s) planned"
                + (trendingStops > 0 ? ", including " + trendingStops + " trending pick(s)." : ".")
                + (Boolean.TRUE.equals(trip.getWeatherAware()) ? " Weather-aware scheduling enabled." : "")
                + (Boolean.TRUE.equals(trip.getIncludePrayerSchedule()) ? " Prayer-aware timing enabled." : "");
    }

    private List<String> buildQuickNotes(Trip trip, List<TripPlace> orderedTripPlaces) {
        List<String> notes = new ArrayList<>();
        if (Boolean.TRUE.equals(trip.getIncludeTopRatings())) {
            notes.add("Google ratings are prioritized");
        }
        if (Boolean.TRUE.equals(trip.getIncludeTikTokTrending())) {
            notes.add("TikTok trending places are prioritized");
        }
        if (Boolean.TRUE.equals(trip.getWeatherAware())) {
            notes.add("Weather-aware stop selection is enabled");
        }
        if (Boolean.TRUE.equals(trip.getIncludePrayerSchedule())) {
            notes.add("Prayer-aware timing is enabled");
        }
        if (orderedTripPlaces != null && !orderedTripPlaces.isEmpty()) {
            notes.add("Ready for map pins and replace-stop interactions");
        }
        return notes;
    }

    private String buildPlaceSubtitle(Place place) {
        List<String> parts = new ArrayList<>();
        if (place.getCategory() != null && !place.getCategory().isBlank()) {
            parts.add(capitalize(place.getCategory().replace('_', ' ')));
        }
        if (place.getVibeTag() != null && !place.getVibeTag().isBlank()) {
            parts.add(capitalize(place.getVibeTag()));
        }
        if (place.getCity() != null && place.getCity().getName() != null) {
            parts.add(place.getCity().getName());
        }
        return parts.isEmpty() ? "Trip stop" : String.join(" • ", parts);
    }

    private String buildMapsUrl(Place place) {
        if (place.getGoogleMapsUrl() != null && !place.getGoogleMapsUrl().isBlank()) {
            return place.getGoogleMapsUrl();
        }
        if (place.getLatitude() != null && place.getLongitude() != null) {
            return "https://www.google.com/maps/search/?api=1&query=" + place.getLatitude() + "," + place.getLongitude();
        }
        return null;
    }

    private TripResponse toTripResponse(Trip trip) {
        List<TripPlace> tripPlaces = trip.getTripPlaces();
        if (tripPlaces == null && trip.getId() != null) {
            tripPlaces = tripPlaceRepository.findAllByTripIdOrderByDayNumberAscOrderInDayAsc(trip.getId());
        }

        return new TripResponse(
                trip.getId(),
                trip.getTitle(),
                trip.getStartDate(),
                trip.getEndDate(),
                trip.getDaysCount(),
                trip.getGroupSize(),
                trip.getTripType(),
                trip.getBudgetTier(),
                trip.getTotalCostEstimate(),
                trip.getIncludeTikTokTrending(),
                trip.getWeatherAware(),
                trip.getIncludePrayerSchedule(),
                trip.getIncludeTopRatings(),
                trip.getUserPrompt(),
                trip.getAiItineraryLogic(),
                trip.getCreatedAt(),
                trip.getUser() != null ? trip.getUser().getId() : null,
                trip.getCity() != null ? trip.getCity().getId() : null,
                trip.getCity() != null ? trip.getCity().getName() : null,
                tripPlaces != null ? tripPlaces.size() : 0
        );
    }

    private Trip toTrip(TripRequest request) {
        Trip trip = new Trip();
        applyRequestToTrip(trip, request);
        return trip;
    }

    private void applyRequestToTrip(Trip trip, TripRequest request) {
        trip.setTitle(request.getTitle());
        trip.setStartDate(request.getStartDate());
        trip.setEndDate(request.getEndDate());
        trip.setDaysCount(request.getDaysCount());
        trip.setGroupSize(request.getGroupSize());
        trip.setTripType(request.getTripType());
        trip.setBudgetTier(request.getBudgetTier());
        trip.setTotalCostEstimate(request.getTotalCostEstimate());
        trip.setIncludeTikTokTrending(request.getIncludeTikTokTrending());
        trip.setWeatherAware(request.getWeatherAware());
        trip.setIncludePrayerSchedule(request.getIncludePrayerSchedule());
        trip.setIncludeTopRatings(request.getIncludeTopRatings());
        trip.setUserPrompt(request.getUserPrompt());
        trip.setUser(resolveUser(request.getUserId()));
        trip.setCity(resolveCity(request.getCityId()));
    }

    private void mergeIntoExistingTrip(Trip target, Trip source) {
        target.setTitle(source.getTitle());
        target.setStartDate(source.getStartDate());
        target.setEndDate(source.getEndDate());
        target.setDaysCount(source.getDaysCount());
        target.setGroupSize(source.getGroupSize());
        target.setTripType(source.getTripType());
        target.setBudgetTier(source.getBudgetTier());
        target.setTotalCostEstimate(source.getTotalCostEstimate());
        target.setIncludeTikTokTrending(source.getIncludeTikTokTrending());
        target.setWeatherAware(source.getWeatherAware());
        target.setIncludePrayerSchedule(source.getIncludePrayerSchedule());
        target.setIncludeTopRatings(source.getIncludeTopRatings());
        target.setUserPrompt(source.getUserPrompt());
        target.setAiItineraryLogic(source.getAiItineraryLogic());
        target.setUser(source.getUser());
        target.setCity(source.getCity());
    }

    private User resolveUser(Integer userId) {
        return userRepository.findById(userId)
                .orElseThrow(() -> new ApiException("User not found"));
    }

    private City resolveCity(Integer cityId) {
        return cityRepository.findById(cityId)
                .orElseThrow(() -> new ApiException("City not found"));
    }


}
