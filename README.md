**YouTube Url Extractor**

A Youtube url extractor for Java, Kotlin and Android for streaming and downloading purpose.

## Features 
- Extracts Mixed and Adaptive urls separately
- Extracts Signature Protected Videos(like vevo)
- Extracts Live Videos Urls(hls) 
- Extracts video info(title,author,description,view,etc)
- Extracts Age restricted videos (Uses Cookie from a Google account)
- Extracts YouTube Video Captions

### Usage
In the app module:
```
implementation 'com.github.Andre-max:YouTube-Url-Extractor:0.1.4'
```

In the project gradle:
```
allprojects {
    repositories {
        google()
        jcenter()
        maven { url'https://jitpack.io' }
    }
}
```


## Dependencies Used 
- Gson
- Mozilla Rhino
- UniversalVideoView(Used only for video testing)

Usage

```Java
new YoutubeStreamExtractor(new YoutubeStreamExtractor.ExtractorListner(){
				@Override
				public void onExtractionDone(List<YTMedia> adaptiveStream, final List<YTMedia> mixedStream,List<YTSubtitles> subtitles, YoutubeMeta meta) {
					//url to get subtitle
					String subUrl=subtitles.get(0).getBaseUrl();
					for (YTMedia media:adativeStream) {
						if(media.isVideo()){
							//is video
						}else{
							//is audio
						}
					}
				}
				@Override
				public void onExtractionGoesWrong(final ExtractorException e) {
					Toast.makeText(getApplicationContext(), e.getMessage(), Toast.LENGTH_LONG).show();
				}
			}).useDefaultLogin().Extract(URL/YOUTUBE_ID);
             //use .useDefaultLogin() to extract age restricted videos 

```

```Kotlin
val EXAMPLE_URL = "https://www.youtube.com/watch?v=RnJnNru9I78"

 val streamExtractorListener = object : YoutubeStreamExtractor.ExtractorListner {
            override fun onExtractionGoesWrong(p0: ExtractorException?) {
                Log.e("MainActivity", "$p0")
                Toast.makeText(applicationContext, "$p0", Toast.LENGTH_SHORT).show()
            }

            override fun onExtractionDone(p0: MutableList<YTMedia>?,p1: MutableList<YTMedia>?,p2: MutableList<YTSubtitles>?,p3: YoutubeMeta?) {
                val subUrl = p2?.get(0)?.baseUrl                

                if (p0 == null) Log.e(TAG, "p0 is null")
                if (p1 == null) Log.e(TAG, "p1 is null")

                newDownloadUrl = p1?.get(0)?.url
                videoData = p3

                val notAllowedSymbols = "[\\\\><\"|*?%:#/]"
                filename = videoData?.title ?: "Downloading"
                filename = filename.filter { char -> !notAllowedSymbols.contains(char) }
                Log.i(TAG, "filename is $filename")

                Log.i(TAG, "newDownloadUrl is $newDownloadUrl")
                downloadFromUrl(newDownloadUrl, videoData?.title, filename)
            }
        }

        val streamExtractor = YoutubeStreamExtractor(streamExtractorListener)
        streamExtractor.useDefaultLogin().Extract(EXAMPLE_URL)
```




## For age restricted Videos

For extraction of age restricted videos use `useDefaultLogin()` to use default cookie... or you can override with your own cookies by method `setHeaders` 
