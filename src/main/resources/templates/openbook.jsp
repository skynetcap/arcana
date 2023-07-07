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
                    <a class="nav-link active" aria-current="page" href="../openbook">OpenBook</a>
                </li>
                <li class="nav-item">
                    <a class="nav-link" href="#">Drift</a>
                </li>
                <li class="nav-item">
                    <a class="nav-link" href="../settings">Settings</a>
                </li>
            </ul>
            <div class="d-flex navbar-text">
                RPC: <span th:text="${rpcEndpoint}" style="padding-left: 4px"></span>
            </div>
        </div>
    </div>
</nav>

<main class="container">
    <div class="bg-light p-5 rounded">
        <h2>OpenBook Markets</h2>
        <table class="table">
            <thead>
            <tr>
                <th scope="col">#</th>
                <th scope="col">Base Token</th>
                <th scope="col">Quote Token</th>
                <th scope="col">Base Deposits Total</th>
                <th scope="col">Add Strategy</th>
            </tr>
            </thead>
            <tbody>
            <tr>
                <th scope="row">1</th>
                <td>OpenBook</td>
                <td>SOL/USDC</td>
                <td>Spread</td>
                <td><a href="#" class="btn btn-primary">Add Strategy</a></td>
            </tr>
            <tr>
                <th scope="row">2</th>
                <td>OpenBook</td>
                <td>ETH/USDC</td>
                <td>Spread</td>
                <td><a href="#" class="btn btn-primary">Add Strategy</a></td>
            </tr>
            <tr>
                <th scope="row">3</th>
                <td>Drift</td>
                <td>SOL-PERP</td>
                <td>Spread</td>
                <td><a href="#" class="btn btn-primary">Add Strategy</a></td>
            </tr>
            </tbody>
        </table>
    </div>
</main>


<script src="../static/bootstrap.bundle.min.js"></script>


</body>
</html>
