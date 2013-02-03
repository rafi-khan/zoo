$(document).ready ->
  $.fn.smartbg = (url, time, cb) ->
    t = this
    # create an img so the browser will download the image: 
    $("<img />").attr("src", url).load ->
      $(t).css "backgroundImage", "url(#{url})"
      $(t).fadeIn time, ->
        cb t if typeof cb is "function"
    this
  # Setup
  bImages = [
    "cardinal.jpg",
    "chameleon.jpg",
    "frog.jpg",
    "giraffe.jpg",
    "grizzly.jpg",
    "hare.jpg",
    "hawk.jpg",
    "hippo.jpg",
    "jaguar.jpg",
    "ladybug.jpg",
    "monkey.jpg"
    "lion.jpg",
    "rhino.jpg",
    "swan.jpg",
    "tiger.jpg"
    "turtle.jpg",
    "peacock.jpg",
    "zebra.jpg"
  ]
  backs = $(".back")
  for b, i in bImages
    $(backs[i]).smartbg "/images/backgrounds/#{bImages[i]}", 50

  num = Math.floor(Math.random()*bImages.length)
  $(backs[num]).addClass "shown"
  setInterval ->
    $(".back.shown").removeClass "shown"
    num = Math.floor(Math.random()*bImages.length)
    $(backs[num]).addClass "shown"
  , 10000

  $(window).scroll ->
    if $("body").scrollTop() < $(window).height() - 40 
      $("#zoo-nav").slideUp("fast")
    else
      $("#zoo-nav").slideDown("fast")

  $(".js-nav").click ->
    tar = $(this).attr "target"
    dest = $(".panel[name=#{tar}]").offset().top
    $("body,html,document").animate
        scrollTop: dest-40
      , 300

  animals = ["aphid", "bumblebee", "cardinal", "chameleon", "cicada", "cobra", "cricket", "frog", "gator", "giraffe", "grizzly", "hare", "hawk", "hippo", "hornet", "jaguar", "ladybug", "lion", "macaw", "monkey", "newt", "peacock", "perch", "python", "rattlesnake", "raven", "rhino", "scorpion", "swan", "termite", "tick", "tiger", "turtle", "viper", "woodpecker", "zebra"] 
  used = {}
  $.get("http://bumblebee.zoo.cs.yale.edu:6789/zoo", (data) ->
    if data
      lines = data.split "\n"
      console.log "Number of lines: #{lines.length}"
      for line in lines
        entries = line.split "|"
        if entries.length is 8
          node = entries[6].replace /^\s+|\s+$/g, ""
          if node in animals 
            ip = entries[5]
            if ip and /\./.test ip
              used[node] = true
      for key,val of used
        
    else
      console.log "request failed..."
  )
