<!doctype html>
<html lang="en">
<head>
    <meta charset="utf-8">
    <meta name="viewport" content="width=device-width, initial-scale=1">
    <meta name="description" content="">
    <meta name="author" content="Mark Otto, Jacob Thornton, and Bootstrap contributors">
    <meta name="generator" content="Hugo 0.84.0">
    <title>🧙‍♂️ Arcana on Solana</title>

    <link rel="canonical" href="https://getbootstrap.com/docs/5.0/examples/navbar-fixed/">


    <!-- Bootstrap core CSS -->
    <link href="../static/bootstrap.min.css" rel="stylesheet">

    <style>
        .bd-placeholder-img {
            font-size: 1.125rem;
            text-anchor: middle;
            -webkit-user-select: none;
            -moz-user-select: none;
            user-select: none;
        }

        @media (min-width: 768px) {
            .bd-placeholder-img-lg {
                font-size: 3.5rem;
            }
        }
    </style>


    <!-- Custom styles for this template -->
    <link href="../static/navbar-top-fixed.css" rel="stylesheet">
</head>
<body>

<nav class="navbar navbar-expand-md navbar-dark fixed-top bg-dark">
    <div class="container-fluid">
        <a class="navbar-brand" href="#">🧙‍♂️ Arcana</a>
        <button class="navbar-toggler" type="button" data-bs-toggle="collapse" data-bs-target="#navbarCollapse"
                aria-controls="navbarCollapse" aria-expanded="false" aria-label="Toggle navigation">
            <span class="navbar-toggler-icon"></span>
        </button>
        <div class="collapse navbar-collapse" id="navbarCollapse">
            <ul class="navbar-nav me-auto mb-2 mb-md-0">
                <li class="nav-item">
                    <a class="nav-link" href="../">Home</a>
                </li>
                <li class="nav-item">
                    <a class="nav-link" href="#">✨ Strategies</a>
                </li>
                <li class="nav-item">
                    <a class="nav-link" href="#">OpenBook</a>
                </li>
                <li class="nav-item">
                    <a class="nav-link" href="#">Drift</a>
                </li>
                <li class="nav-item">
                    <a class="nav-link active" aria-current="page" href="../settings">Settings</a>
                </li>
            </ul>
        </div>
    </div>
</nav>

<main class="container">
    <div class="bg-light p-5 rounded">
        <a class="btn btn-lg btn-success" href="../components/navbar/" role="button">✨ Strategies &raquo;</a>
        <a class="btn btn-lg btn-primary" href="../components/navbar/" role="button">View OpenBook markets &raquo;</a>
        <a class="btn btn-lg btn-primary" href="../components/navbar/" role="button">View Drift markets &raquo;</a>
        <hr>
        <h2>Settings</h2>
        <form>
            <div class="input-group mb-3">
                <div class="input-group-prepend">
                    <span class="input-group-text" id="basic-addon3">RPC Server</span>
                </div>
                <input type="text" class="form-control" id="basic-url" aria-describedby="basic-addon3">
                <input type="submit" class="btn btn-primary"/>
            </div>
        </form>
    </div>
</main>


<script src="../static/bootstrap.bundle.min.js"></script>


</body>
</html>
