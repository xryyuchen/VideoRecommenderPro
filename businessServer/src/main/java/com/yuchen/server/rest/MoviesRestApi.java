package com.yuchen.server.rest;

import com.yuchen.java.model.Constant;
import com.yuchen.server.model.core.Movie;
import com.yuchen.server.model.core.Rating;
import com.yuchen.server.model.core.Tag;
import com.yuchen.server.model.core.User;
import com.yuchen.server.model.recom.Recommendation;
import com.yuchen.server.model.request.*;
import com.yuchen.server.service.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

/**
 * @author yuchen
 * 用户处理电影相关的功能
 */
@Controller
@RequestMapping("/rest/movies")
public class MoviesRestApi {

    @Autowired
    private RecommenderService recommenderService;

    @Autowired
    private UserService userService;

    @Autowired
    private MovieService movieService;

    @Autowired
    private TagService tagService;

    @Autowired
    private RatingService ratingService;

    private Logger logger = LoggerFactory.getLogger(MoviesRestApi.class);

    /************** 首页 ********************/

    /**
     * 提供获取实时推荐信息的接口【混合推荐】   需要考虑  冷启动问题
     * 访问：url： /rest/movies/stream?username=abc&num=100
     * 返回：{success:true, movies:[]}
     * @param username
     * @param model
     * @return
     */
    @RequestMapping(path = "/stream", produces = "application/json", method = RequestMethod.GET)
    @ResponseBody
    public Model getRealtimeRecommendations(@RequestParam("username") String username, @RequestParam("num") int num, Model model) {
        User user = userService.findUserByUsername(username);
        List<Recommendation> recommendations = recommenderService.getSteamRecsMovies(new GetStreamRecsRequest(user.getUid(), num));
        if (recommendations.size() == 0) {
            Random random = new Random();
            recommendations = recommenderService.getGenresTopMovies(new GetGenresTopMoviesRequest(user.getGenres().get(random.nextInt(user.getGenres().size())), num));
        }
        List<Integer> ids = new ArrayList<>();
        for (Recommendation recom : recommendations) {
            ids.add(recom.getMid());
        }
        List<Movie> result = movieService.getMoviesByMids(ids);
        model.addAttribute("success", true);
        model.addAttribute("movies", result);
        return model;
    }


    /**
     * 提供获取离线推荐信息
     * @param username
     * @param model
     * @return
     */@RequestMapping(path = "/offline", produces = "application/json", method = RequestMethod.GET)
    @ResponseBody
    public Model getOfflineRecommendations(@RequestParam("username") String username, @RequestParam("num") int num, Model model) {
        User user = userService.findUserByUsername(username);
        List<Recommendation> recommendations = recommenderService.getUserCFMovies(new GetUserCFRequest(user.getUid(), num));
        if (recommendations.size() == 0) {
            Random random = new Random();
            recommendations = recommenderService.getGenresTopMovies(new GetGenresTopMoviesRequest(user.getGenres().get(random.nextInt(user.getGenres().size())), num));
        }
        List<Integer> ids = new ArrayList<>();
        for (Recommendation recom : recommendations) {
            ids.add(recom.getMid());
        }
        List<Movie> result = movieService.getMoviesByMids(ids);
        model.addAttribute("success", true);
        model.addAttribute("movies", result);
        return model;
    }

    /**
     * 提供获取热门推荐信息
     * @param model
     * @return
     */
    @RequestMapping(path = "/host", produces = "application/json", method = RequestMethod.GET)
    @ResponseBody
    public Model getHotRecommendations(@RequestParam("num") int num, Model model) {
        model.addAttribute("success", true);
        model.addAttribute("movies", recommenderService.getHotRecommendations(new GetHotRecommendationRequest(num)));
        return model;
    }

    /**
     * 提供获取优质电影的信息
     * @param model
     * @return
     */
    @RequestMapping(path = "/rate", produces = "application/json", method = RequestMethod.GET)
    @ResponseBody
    public Model getRateMoreRecommendations(@RequestParam("num") int num, Model model) {
        model.addAttribute("success", true);
        model.addAttribute("movies", recommenderService.getRateMoreMovies(new GetRateMoreMoviesRequest(num)));
        return model;
    }

    /**
     * 获取最新电影的信息
     * @param model
     * @return
     */
    @RequestMapping(path = "/new", produces = "application/json", method = RequestMethod.GET)
    @ResponseBody
    public Model getNewRecommendations(@RequestParam("num") int num, Model model) {
        model.addAttribute("success", true);
        model.addAttribute("movies", recommenderService.getNewMovies(new GetNewMoviesRequest(num)));
        return model;
    }

    /**
     * 提供基于名称或者描述的模糊检索功能
     * @param query
     * @param model
     * @return
     */
    @RequestMapping(path = "/query", produces = "application/json", method = RequestMethod.GET)
    @ResponseBody
    public Model getFuzzySearchMovies(@RequestParam("query") String query, @RequestParam("num") int num, Model model) {
        model.addAttribute("success", true);
        model.addAttribute("movies", recommenderService.getFuzzyMovies(new GetFuzzySearchMoviesRequest(query, num)));
        return model;
    }

    /**
     * 获取单个电影的信息
     * @param mid
     * @param model
     * @return
     */
    public Model getMovieInfo(@PathVariable("mid") int mid, Model model) {
        model.addAttribute("success", true);
        model.addAttribute("movie", movieService.findMovieInfo(mid));
        return model;
    }

    /**
     * 需要提供给电影打标签的功能
     * @param mid
     * @param tagname
     * @param model
     * @return
     */
    @RequestMapping(path = "/addtag/{mid}", produces = "application/json", method = RequestMethod.GET)
    @ResponseBody
    public void addTagToMovie(@PathVariable("mid") int mid, @RequestParam("username")String username, @RequestParam("tagname") String tagname, Model model) {
        User user = userService.findUserByUsername(username);
        Tag tag = new Tag(user.getUid(),mid,tagname,System.currentTimeMillis() / 1000);
        tagService.addTagToMovie(tag);
    }

    /**
     * 需要能够获取电影的所有标签信息
     * @param mid
     * @param model
     * @return
     */
    @RequestMapping(path = "/tags/{mid}", produces = "application/json", method = RequestMethod.GET)
    @ResponseBody
    public Model getMovieTags(@PathVariable("mid")int mid, Model model) {
        model.addAttribute("success", true);
        model.addAttribute("movie", tagService.getMovieTags(mid));
        return model;
    }

    /**
     * 需要能够获取电影相似电影的推荐
     * @param mid
     * @param model
     * @return
     */
    @RequestMapping(path = "/same/{mid}", produces = "application/json", method = RequestMethod.GET)
    @ResponseBody
    public Model getSimMoviesRecommendation(@PathVariable("mid") int mid, @RequestParam("num") int num, Model model) {
        model.addAttribute("success", true);
        model.addAttribute("movie", recommenderService.getHybridRecommendations(new GetHybridRecommendationRequest(0.5,mid,num)));
        return model;
    }

    /**
     * 需要提供给电影大评分的功能
     * @param mid
     * @param score
     * @param model
     * @return
     */
    @RequestMapping(path = "/rate/{mid}", produces = "application/json", method = RequestMethod.GET)
    @ResponseBody
    public void rateMovie(@RequestParam("username") String username, @PathVariable("mid") int mid, @RequestParam("score") Double score, Model model) {
        User user = userService.findUserByUsername(username);
        Rating rating = new Rating(user.getUid(),mid,score,System.currentTimeMillis() / 1000);
        ratingService.rateToMovie(rating);
        //输出埋点日志
        logger.info(Constant.USER_RATING_LOG_PREFIX+rating.getUid()+"|"+rating.getMid()+"|"+rating.getScore()+"|"+rating.getTimestamp());
    }

    /**
     * 需要提供影片类别查找的功能
     * @param genres
     * @param model
     * @return
     */
    @RequestMapping(path = "/genres", produces = "application/json", method = RequestMethod.GET)
    @ResponseBody
    public Model getGenresMovies(@RequestParam("genres") String genres, @RequestParam("num") int num, Model model) {
        model.addAttribute("success", true);
        model.addAttribute("movie", recommenderService.getGenresMovies(new GetGenresMoviesRequest(genres,num)));
        return model;
    }

    /**
     * 需要提供用户的所有电影评分记录
     * @param username
     * @param model
     * @return
     */
    public Model getUserRatings(String username,Model model){
        return null;
    }

    /**
     * 需要能够获取图表数据
     * @param username
     * @param model
     * @return
     */
    public Model getUserChart(String username,Model model){
        return null;
    }
}









