package com.example.ui.screens

import android.annotation.SuppressLint
import android.webkit.WebResourceRequest
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView

@SuppressLint("SetJavaScriptEnabled")
@Composable
fun WorldMapChart(
    countryData: List<Pair<String, Double>>,
    modifier: Modifier = Modifier
) {
    val dataRows = countryData.map { "['${it.first}', ${it.second}]" }.joinToString(", ")
    
    val htmlContent = """
        <!DOCTYPE html>
        <html>
        <head>
            <script type="text/javascript" src="https://www.gstatic.com/charts/loader.js"></script>
            <script type="text/javascript">
                google.charts.load('current', {
                    'packages':['geochart']
                });
                google.charts.setOnLoadCallback(drawRegionsMap);

                function drawRegionsMap() {
                    var data = google.visualization.arrayToDataTable([
                        ['Country', 'Trafik (%)'],
                        $dataRows
                    ]);

                    var options = {
                        backgroundColor: '#1E232B',
                        datalessRegionColor: '#2C3440',
                        defaultColor: '#1A55ED',
                        colorAxis: {colors: ['#1A55ED', '#4B7AF2']},
                        legend: 'none',
                        tooltip: {textStyle: {color: '#FFFFFF', fontName: 'Inter'}, isHtml: false, trigger: 'focus'}
                    };

                    var chart = new google.visualization.GeoChart(document.getElementById('regions_div'));
                    chart.draw(data, options);
                }
            </script>
            <style>
                body { margin: 0; padding: 0; background-color: #1E232B; overflow: hidden; }
                #regions_div { width: 100vw; height: 100vh; }
                path { stroke: #15191F !important; stroke-width: 0.5px; transition: fill 0.3s ease; }
                .google-visualization-tooltip { background-color: #15191F !important; border: 1px solid #2C3440 !important; color: #FFFFFF !important; border-radius: 8px !important; box-shadow: 0 4px 12px rgba(0,0,0,0.5) !important; padding: 8px !important;}
            </style>
        </head>
        <body>
            <div id="regions_div"></div>
        </body>
        </html>
    """.trimIndent()

    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(240.dp)
            .clip(RoundedCornerShape(8.dp))
            .background(MaterialTheme.colorScheme.surfaceVariant),
        contentAlignment = Alignment.Center
    ) {
        if (countryData.isEmpty()) {
            Text("Harita verisi bulunamadı", color = Color.Gray, fontSize = 12.sp)
        } else {
            AndroidView(
                factory = { context ->
                    WebView(context).apply {
                        settings.javaScriptEnabled = true
                        settings.domStorageEnabled = true
                        webViewClient = object : WebViewClient() {
                            override fun shouldOverrideUrlLoading(view: WebView?, request: WebResourceRequest?): Boolean {
                                return true
                            }
                        }
                        setBackgroundColor(android.graphics.Color.TRANSPARENT)
                        loadDataWithBaseURL(null, htmlContent, "text/html", "UTF-8", null)
                    }
                },
                update = { webView ->
                    webView.loadDataWithBaseURL(null, htmlContent, "text/html", "UTF-8", null)
                },
                modifier = Modifier.fillMaxSize()
            )
        }
    }
}
