package com.andre_max.youtube_url_extractor;

import android.os.AsyncTask;
import android.os.Handler;
import android.os.Looper;

import com.andre_max.youtube_url_extractor.model.PlayerResponse;
import com.andre_max.youtube_url_extractor.model.Response;
import com.andre_max.youtube_url_extractor.model.StreamingData;
import com.andre_max.youtube_url_extractor.model.YTMedia;
import com.andre_max.youtube_url_extractor.model.YTSubtitles;
import com.andre_max.youtube_url_extractor.model.YoutubeMeta;
import com.andre_max.youtube_url_extractor.utils.HTTPUtility;
import com.andre_max.youtube_url_extractor.utils.LogUtils;
import com.andre_max.youtube_url_extractor.utils.RegexUtils;
import com.andre_max.youtube_url_extractor.utils.Utils;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonParser;

import java.net.URLDecoder;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

public class YoutubeStreamExtractor extends AsyncTask<String,Void,Void> {


	Map<String,String> Headers=new HashMap<>();
	List<YTMedia> adaptiveMedia=new ArrayList<>();
	List<YTMedia> mixedMedia =new ArrayList<>();
	List<YTSubtitles> subtitle=new ArrayList<>();
	String regexUrl=("(?<=url=).*");
	String regexYtshortLink="(http|https)://(www\\.|)youtu.be/.*";
	String regexPageLink = ("(http|https)://(www\\.|m.|)youtube\\.com/watch\\?v=(.+?)( |\\z|&)");
	String regexFindReason="(?<=(class=\"message\">)).*?(?=<)";
	String regexPlayerJson="(?<=ytplayer.config\\s=).*?((\\}(\n|)\\}(\n|))|(\\}))(?=;)";
	ExtractorListener listener;
	private ExtractorException Ex;
	List<String> reasonUnavialable=Arrays.asList("This video is unavailable on this device.","Content Warning","who has blocked it on copyright grounds.");
	Handler han=new Handler(Looper.getMainLooper());
	private Response response;
	private YoutubeMeta ytmeta;






	public YoutubeStreamExtractor(ExtractorListener EL) {
		this.listener = EL;
		Headers.put("Accept-Language", "en");
	}

	public YoutubeStreamExtractor setHeaders(Map<String, String> headers) {
		Headers = headers;
		return this;
	}

	public YoutubeStreamExtractor useDefaultLogin() {
		Headers.put("Cookie", Utils.loginCookie);
		return setHeaders(Headers);	
	}

	public Map<String, String> getHeaders() {
		return Headers;
	}

	public void Extract(String VideoId) {
		this.execute(VideoId);
	}



	@Override
	protected void onPostExecute(Void result) {
		if (Ex != null) {
			listener.onExtractionGoesWrong(Ex);
		} else {
			listener.onExtractionDone(adaptiveMedia, mixedMedia,subtitle, ytmeta);
		}
	}

	@Override
	protected void onPreExecute() {
		Ex = null;
		adaptiveMedia.clear();
		mixedMedia.clear();

	}

	@Override
	protected void onCancelled() {
		if (Ex != null) {
			listener.onExtractionGoesWrong(Ex);
		}	
	}



	@Override
	protected Void doInBackground(String[] ids) {

		String Videoid=Utils.extractVideoID(ids[0]);
        String jsonBody;
        try {
			String body = HTTPUtility.downloadPageSource("https://www.youtube.com/watch?v=" + Videoid + "&has_verified=1&bpctr=9999999999", Headers);
			jsonBody = parsePlayerConfig(body);

			PlayerResponse playerResponse=parseJson(jsonBody);
			ytmeta = playerResponse.getVideoDetails();
			subtitle=playerResponse.getCaptions() !=null ? playerResponse.getCaptions().getPlayerCaptionsTracklistRenderer().getCaptionTracks(): null;
			//Utils.copyToBoard(jsonBody);
			if (playerResponse.getVideoDetails().getisLive()) {
				parseLiveUrls(playerResponse.getStreamingData());
			} else {
				StreamingData sd=playerResponse.getStreamingData();
				LogUtils.log("sizea= " + sd.getAdaptiveFormats().length);
				LogUtils.log("sizem= " + sd.getFormats().length);

				adaptiveMedia =	parseUrls(sd.getAdaptiveFormats());
				mixedMedia =	parseUrls(sd.getFormats());
				LogUtils.log("sizeXa= " + adaptiveMedia.size());
				LogUtils.log("sizeXm= " + mixedMedia.size());

			}
		}
		catch (Exception e) {
			LogUtils.log(Arrays.toString(e.getStackTrace()));
			Ex = new ExtractorException("Error While getting Youtube Data:" + e.getMessage());
			this.cancel(true);
		}
		return null;
	}

	/*this function creates Json models using Gson*/
	private PlayerResponse parseJson(String body) throws Exception {
		JsonParser parser=new JsonParser();
		response = new GsonBuilder().serializeNulls().create().fromJson(parser.parse(body), Response.class);
		return new GsonBuilder().serializeNulls().create().fromJson(response.getArgs().getPlayerResponse(), PlayerResponse.class);
	}

	/*This function is used to check if webpage contain stream data and then gets the Json part of from the page using regex*/
	private String parsePlayerConfig(String body) throws ExtractorException {

		if (Utils.isListContain(reasonUnavialable, RegexUtils.matchGroup(regexFindReason, body))) {
			throw new ExtractorException(RegexUtils.matchGroup(regexFindReason, body));
		}
		if (body.contains("ytplayer.config")) {
			return RegexUtils.matchGroup(regexPlayerJson, body);
		} else {
			throw new ExtractorException("This Video is unavailable");
		}
	}


	/*independent function Used to parse urls for adaptive & mixed stream with cipher protection*/

	private List<YTMedia> parseUrls(YTMedia[] rawMedia) {
		List<YTMedia> links=new ArrayList<>();
		try {
            for (YTMedia media : rawMedia) {
                LogUtils.log(media.getSignatureCipher() != null ? media.getSignatureCipher() : "null cip");

                if (media.useCipher()) {
                    String tempUrl = "";
                    String decodedSig = "";
                    for (String partCipher : media.getSignatureCipher().split("&")) {


                        if (partCipher.startsWith("s=")) {
                            decodedSig = CipherManager.dechiperSig(URLDecoder.decode(partCipher.replace("s=", "")), response.getAssets().getJs());
                        }

                        if (partCipher.startsWith("url=")) {
                            tempUrl = URLDecoder.decode(partCipher.replace("url=", ""));

                            for (String url_part : tempUrl.split("&")) {
                                if (url_part.startsWith("s=")) {
                                    decodedSig = CipherManager.dechiperSig(URLDecoder.decode(url_part.replace("s=", "")), response.getAssets().getJs());
                                }
                            }
                        }
                    }

                    String FinalUrl = tempUrl + "&sig=" + decodedSig;

                    media.setUrl(FinalUrl);


                }
                links.add(media);
            }

		}
		catch (Exception e) {
			Ex = new ExtractorException(e.getMessage());
			this.cancel(true);
		}
		return links;
	}





	/*This function parse live youtube videos links from streaming data  */

	private void parseLiveUrls(StreamingData streamData) throws Exception {
		if (streamData.getHlsManifestUrl() == null) {
			throw new ExtractorException("No link for hls video");
		}
		String hlsPageSource=HTTPUtility.downloadPageSource(streamData.getHlsManifestUrl());
		String regexhlsLinks="(#EXT-X-STREAM-INF).*?(index.m3u8)";
		List<String> rawData= RegexUtils.getAllMatches(regexhlsLinks, hlsPageSource);
		for (String data:rawData) {
			YTMedia media=new YTMedia();
			String[] info_list= Objects.requireNonNull(RegexUtils.matchGroup("(#).*?(?=https)", data)).split(",");
			String live_url=RegexUtils.matchGroup("(https:).*?(index.m3u8)", data);
			media.setUrl(live_url);
			for (String info:info_list) {
				if (info.startsWith("BANDWIDTH")) {
					media.setBitrate(Integer.parseInt(info.replace("BANDWIDTH=", "")));
				}
				if (info.startsWith("CODECS")) {
					media.setMimeType((info.replace("CODECS=", "").replace("\"", "")));
				}
				if (info.startsWith("FRAME-RATE")) {
					media.setFps(Integer.parseInt((info.replace("FRAME-RATE=", ""))));
				}
				if (info.startsWith("RESOLUTION")) {
					String[] RESOLUTION= info.replace("RESOLUTION=", "").split("x");
					media.setWidth(Integer.parseInt(RESOLUTION[0]));
					media.setHeight(Integer.parseInt(RESOLUTION[1]));
					media.setQualityLabel(RESOLUTION[1] + "p");
				}
			}
			LogUtils.log(media.getUrl());
			mixedMedia.add(media);
		}


	}

	public interface ExtractorListener {
		void onExtractionGoesWrong(ExtractorException e);
		void onExtractionDone(List<YTMedia> adaptiveStream, List<YTMedia> mixedStream, List<YTSubtitles> subList, YoutubeMeta meta);
	}

}     
