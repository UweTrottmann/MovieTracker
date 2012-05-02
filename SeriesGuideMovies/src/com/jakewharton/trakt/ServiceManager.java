package com.jakewharton.trakt;

import com.jakewharton.trakt.services.AccountService;
import com.jakewharton.trakt.services.ActivityService;
import com.jakewharton.trakt.services.CalendarService;
import com.jakewharton.trakt.services.FriendsService;
import com.jakewharton.trakt.services.GenreService;
import com.jakewharton.trakt.services.ListService;
import com.jakewharton.trakt.services.MovieService;
import com.jakewharton.trakt.services.RateService;
import com.jakewharton.trakt.services.RecommendationsService;
import com.jakewharton.trakt.services.SearchService;
import com.jakewharton.trakt.services.ShoutService;
import com.jakewharton.trakt.services.ShowService;
import com.jakewharton.trakt.services.UserService;

/**
 * Class to manage service creation with default settings.
 *
 * @author Jake Wharton <jakewharton@gmail.com>
 */
public class ServiceManager {
    /** API key. */
    private String apiKeyValue;
    /** User email. */
    private String username;
    /** User password. */
    private String password_sha;
    /** Connection timeout (in milliseconds). */
    private Integer connectionTimeout;
    /** Read timeout (in milliseconds). */
    private Integer readTimeout;
    /** Plugin version debug string. */
    private String pluginVersion;
    /** Media center version debug string. */
    private String mediaCenterVersion;
    /** Media center build date debug string. */
    private String mediaCenterDate;
    /** Whether or not to use SSL API endpoint. */
    private boolean useSsl;


    /** Create a new manager instance. */
    public ServiceManager() {}


    /**
     * Set default authentication credentials.
     *
     * @param username Username.
     * @param password_sha SHA1 of user password.
     * @return Current instance for builder pattern.
     */
    public ServiceManager setAuthentication(String username, String password_sha) {
        this.username = username;
        this.password_sha = password_sha;
        return this;
    }

    /**
     * Set default API key.
     *
     * @param value API key value.
     * @return Current instance for builder pattern.
     */
    public ServiceManager setApiKey(String value) {
        this.apiKeyValue = value;
        return this;
    }

    /**
     * Set default connection timeout.
     *
     * @param connectionTimeout Timeout (in milliseconds).
     * @return Current instance for builder pattern.
     */
    public ServiceManager setConnectionTimeout(int connectionTimeout) {
        this.connectionTimeout = connectionTimeout;
        return this;
    }

    /**
     * Set default read timeout.
     *
     * @param readTimeout Timeout (in milliseconds).
     * @return Current instance for builder pattern.
     */
    public ServiceManager setReadTimeout(int readTimeout) {
        this.readTimeout = readTimeout;
        return this;
    }

    /**
     * Set default debug information when using a developer method.
     *
     * @param pluginVersion Internal version of your plugin. Make sure to
     * increment this for each plugin update.
     * @param mediaCenterVersion Version number of the media center, be as
     * specific as you can including nightly build number, etc.
     * @param mediaCenterDate Build date of the media center.
     * @return Current instance for builder pattern.
     */
    public ServiceManager setDebugInfo(String pluginVersion, String mediaCenterVersion, String mediaCenterDate) {
        this.pluginVersion = pluginVersion;
        this.mediaCenterVersion = mediaCenterVersion;
        this.mediaCenterDate = mediaCenterDate;
        return this;
    }

    /**
     * Set whether or not to use SSL API endpoint.
     *
     * @param useSsl Value.
     * @return Current instance for builder pattern.
     */
    public ServiceManager setUseSsl(boolean useSsl) {
        this.useSsl = useSsl;
        return this;
    }

    /**
     * Set up a new service with the defaults.
     *
     * @param service Service to set up.
     */
    private void setupService(TraktApiService service) {
        if (this.apiKeyValue != null) {
            service.setApiKey(this.apiKeyValue);
        }
        if ((this.username != null) && (this.password_sha != null)) {
            service.setAuthentication(this.username, this.password_sha);
        }
        if (this.connectionTimeout != null) {
            service.setConnectTimeout(this.connectionTimeout);
        }
        if (this.readTimeout != null) {
            service.setReadTimeout(this.readTimeout);
        }
        if (this.pluginVersion != null) {
            service.setPluginVersion(this.pluginVersion);
        }
        if (this.mediaCenterVersion != null) {
            service.setMediaCenterVersion(this.mediaCenterVersion);
        }
        if (this.mediaCenterDate != null) {
            service.setMediaCenterDate(this.mediaCenterDate);
        }
        service.setUseSsl(this.useSsl);
    }

    public AccountService accountService() {
        AccountService service = ServiceManager.createAccountService();
        this.setupService(service);
        return service;
    }

    public ActivityService activityService() {
        ActivityService service = ServiceManager.createActivityService();
        this.setupService(service);
        return service;
    }

    public CalendarService calendarService() {
        CalendarService service = ServiceManager.createCalendarService();
        this.setupService(service);
        return service;
    }

    public FriendsService friendsService() {
        FriendsService service = ServiceManager.createFriendsService();
        this.setupService(service);
        return service;
    }

    public GenreService genreService() {
        GenreService service = ServiceManager.createGenreService();
        this.setupService(service);
        return service;
    }

    public ListService listService() {
        ListService service = ServiceManager.createListService();
        this.setupService(service);
        return service;
    }

    public MovieService movieService() {
        MovieService service = ServiceManager.createMovieService();
        this.setupService(service);
        return service;
    }

    public RateService rateService() {
        RateService service = ServiceManager.createRateService();
        this.setupService(service);
        return service;
    }

    public RecommendationsService recommendationsService() {
        RecommendationsService service = ServiceManager.createRecommendationsService();
        this.setupService(service);
        return service;
    }

    public SearchService searchService() {
        SearchService service = ServiceManager.createSearchService();
        this.setupService(service);
        return service;
    }

    public ShoutService shoutService() {
        ShoutService service = ServiceManager.createShoutService();
        this.setupService(service);
        return service;
    }

    public ShowService showService() {
        ShowService service = ServiceManager.createShowService();
        this.setupService(service);
        return service;
    }

    public UserService userService() {
        UserService service = ServiceManager.createUserService();
        this.setupService(service);
        return service;
    }

    public static final AccountService createAccountService() {
        return new AccountService();
    }

    public static final ActivityService createActivityService() {
        return new ActivityService();
    }

    public static final CalendarService createCalendarService() {
        return new CalendarService();
    }

    public static final FriendsService createFriendsService() {
        return new FriendsService();
    }

    public static final GenreService createGenreService() {
        return new GenreService();
    }

    public static final ListService createListService() {
        return new ListService();
    }

    public static final MovieService createMovieService() {
        return new MovieService();
    }

    public static final RateService createRateService() {
        return new RateService();
    }

    public static final RecommendationsService createRecommendationsService() {
        return new RecommendationsService();
    }

    public static final SearchService createSearchService() {
        return new SearchService();
    }

    public static final ShoutService createShoutService() {
        return new ShoutService();
    }

    public static final ShowService createShowService() {
        return new ShowService();
    }

    public static final UserService createUserService() {
        return new UserService();
    }
}
