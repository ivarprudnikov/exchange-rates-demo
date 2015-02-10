$(function(){
	
	// shared chart data
	var chart = {
		inProcess: false,
		isCreated: false,
		margin: {top: 30, right: 40, bottom: 30, left: 50},
		width:null,
		height:null,
		x:null,
		y:null,
		xAxis:null,
		yAxis:null,
		valueline:null,
		svg:null
	};
	chart.width = $("#currencyGraph").width() - chart.margin.left - chart.margin.right;
	chart.height = 300 - chart.margin.top - chart.margin.bottom;
	
	var selectPrepared = false;
	
	// to see it in console
	window.cache = {
		currency: {}
	};
	
	// initialize
	prepareSelectElement();
	
	
	$('#refresh-client').on('click',function(){
		refreshClient();
	});
	
	$('#refresh-server').on('click',function(){
		refreshServer();
	});
	
	function refreshClient(){
		cache = {
			currency: {}
		};
		prepareSelectElement();
	}
	
	function refreshServer(){
		$.getJSON('/rates/refresh',function(data){
			refreshClient();
		});
	}
	
	function formatResponseData(data){
		// Parse the date / time
		var parseDate = d3.time.format("%Y-%m-%d").parse;
		data.forEach(function(d) {
	        d.t = parseDate(d.time);
	        d.r = +d.rate;
	        d.c = d.currency;
	    });
	}
	
	function getAllRates(cb){
		
		if('function' !== typeof cb){
			throw new Error("callback argument should be function")
		}
		
		if(cache.rates){
			console.log("accessing stored rates data");
			setTimeout(function(){
				cb(cache.rates);
			},20)
		} else {
			console.log("retrieving fresh rates");
			$.getJSON('/rates',function(data){
				formatResponseData(data);
				cache.rates = data;
				cb(cache.rates);
			});
		}
	}
	
	
	function withSelectedCurrency(cb){
		var currencyVal = $("#currencySelector").val();
		if(cache.currency[currencyVal] != null){
			console.log("accessing stored currency",currencyVal);
			setTimeout(function(){
				cb.call(cb,cache.currency[currencyVal]);
			},20);
		} else {
			console.log("filtering currency",currencyVal);
			getAllRates(function(data){
				cache.currency[currencyVal] = $.grep(data, function(o) { return o["currency"] === currencyVal; });
				cb.call(cb,cache.currency[currencyVal]);
			});
		}
	}
	
	
	function prepareSelectElement(){
		
		console.log('preparing select element values');
		
		var el$ = $("#currencySelector");
		el$.attr('disabled','disabled');
		
		// populate select box with available rates
		getAllRates(function(data){
			
			var anyDateValue = data[0].time;
			
			el$.empty();
			
			$.map(data,function(val,i){
				if(val.time === anyDateValue){
					var option$ = $('<option></option>').text(val.currency).attr('value',val.currency);
					option$.appendTo(el$);
				}
			});
			
			if(!selectPrepared){
				el$.on('change', triggerGraph);
				selectPrepared = true;
			}
			el$.val('USD');
			el$.removeAttr('disabled');
			el$.trigger('change');
		});
	}
	
	function triggerGraph(){
		
		if(!chart.isCreated){
			create();
		} else {
			update();
		}
		
		function create(){
			
			console.log('creating svg chart');
			
			if(chart.inProcess) return;
			
			$("#currencySelector").attr('disabled','disabled');
			
			chart.inProcess = true;
			
			// Set the ranges
			chart.x = d3.time.scale().range([0, chart.width]);
			chart.y = d3.scale.linear().range([chart.height, 0]);
			
			chart.xAxis = d3.svg.axis().scale(chart.x)
			    .orient("bottom").ticks(20);

			chart.yAxis = d3.svg.axis().scale(chart.y)
			    .orient("left").ticks(10);

			chart.valueline = d3.svg.line()
				.interpolate("linear")
			    .x(function(d) { return chart.x(d.t); })
			    .y(function(d) { return chart.y(d.r); });
		  
			chart.svg = d3.select("#currencyGraph")
			    .append("svg")
			        .attr("width", chart.width + chart.margin.left + chart.margin.right)
			        .attr("height", chart.height + chart.margin.top + chart.margin.bottom)
			      .append("g")
			        .attr("transform", "translate(" 
			            + chart.margin.left 
			            + "," + chart.margin.top + ")");
			
			
			
			withSelectedCurrency.call(this,function(data){
				
			    // Scale the range of the data
				chart.x.domain(d3.extent(data, function(d) { return d.t; }));
				chart.y.domain([
			        d3.min(data, function(d) { return d.r; }), 
			        d3.max(data, function(d) { return d.r; })
			    ]);
			
				chart.svg.append("path")        // Add the valueline path.
			        .attr("class", "line")
			        .attr("d", chart.valueline(data));
			
				chart.svg.append("g")            // Add the X Axis
			        .attr("class", "x axis")
			        .attr("transform", "translate(0," + chart.height + ")")
			        .call(chart.xAxis);
			
				chart.svg.append("g")            // Add the Y Axis
			        .attr("class", "y axis")
			        .call(chart.yAxis);
			
				chart.svg.append("text")          // Add the label
			        .attr("class", "label")
			        .attr("transform", "translate(" + (chart.width+3) + "," 
			            + chart.y(data[(data.length-1)].r) + ")")
			        .attr("dy", ".35em")
			        .attr("text-anchor", "start")
			        .style("fill", "steelblue")
			        .text(data[(data.length-1)].r);
			
				chart.svg.append("text")          // Add the title shadow
			        .attr("x", (chart.width / 2))
			        .attr("y", chart.margin.top / 2)
			        .attr("text-anchor", "middle")
			        .attr("class", "shadow")
			        .style("font-size", "16px")
			        .text(data[0].c);
			        
				chart.svg.append("text")          // Add the title
			        .attr("class", "stock")
			        .attr("x", (chart.width / 2))
			        .attr("y", chart.margin.top / 2)
			        .attr("text-anchor", "middle")
			        .style("font-size", "16px")
			        .text(data[0].c);
				
			    // cleanup
			    $("#currencySelector").removeAttr('disabled');
			    chart.inProcess = false;
				chart.isCreated = true;
			});
		}
		
		
		function update(){
			
			console.log('updating svg chart');
			
			if(chart.inProcess) return;
			
			$("#currencySelector").attr('disabled','disabled');
			
			chart.inProcess = true;
			
			withSelectedCurrency.call(this,function(data){
				
				// Scale the range of the data
				chart.x.domain(d3.extent(data, function(d) { return d.t; }));
				chart.y.domain([
			        d3.min(data, function(d) { return d.r; }), 
			        d3.max(data, function(d) { return d.r; })
			    ]);
			    
			    // Select the section we want to apply our changes to
		        var svg = d3.select("#currencyGraph").transition();
			    
		        // Make the changes
		        svg.select(".line")    // change the line
		            .duration(750) 
		            .attr("d", chart.valueline(data));

		        svg.select(".label")   // change the label text
		            .duration(750)
		            .attr("transform", "translate(" + (chart.width+3) + "," 
		            + chart.y(data[(data.length-1)].r) + ")")
		            .text(data[(data.length-1)].r);;
		 
		        svg.select(".shadow") // change the title shadow
		            .duration(750)
		            .text(data[0].c);  
		             
		        svg.select(".stock")   // change the title
		            .duration(750)
		            .text(data[0].c);
		     
		        svg.select(".x.axis") // change the x axis
		            .duration(750)
		            .call(chart.xAxis);
		        svg.select(".y.axis") // change the y axis
		            .duration(750)
		            .call(chart.yAxis);
		        
		        // cleanup
			    $("#currencySelector").removeAttr('disabled');
			    chart.inProcess = false;
			});
		}
		
	}
	
});